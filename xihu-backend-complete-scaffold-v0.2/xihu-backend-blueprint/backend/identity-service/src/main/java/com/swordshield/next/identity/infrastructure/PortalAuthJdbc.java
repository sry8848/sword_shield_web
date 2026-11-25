package com.swordshield.next.identity.infrastructure;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import com.swordshield.next.identity.application.PortalAuthentication;
import com.swordshield.next.identity.application.PortalAuthentication.Account;
import com.swordshield.next.identity.application.PortalAuthentication.NewSession;
import com.swordshield.next.identity.application.PortalAuthentication.RefreshState;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class PortalAuthJdbc implements PortalAuthentication.Store {

    private final JdbcClient jdbc;

    PortalAuthJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** 按唯一手机号读取账号的验密和版本信息。 */
    @Override
    public Optional<Account> findByPhone(String phone) {
        return jdbc.sql("""
                SELECT id, password_hash, status, credential_version, permission_version
                FROM identity_account WHERE phone = :phone
                """)
                .param("phone", phone)
                .query((rs, row) -> new Account(rs.getString("id"), rs.getString("password_hash"),
                        "ENABLED".equals(rs.getString("status")), rs.getInt("credential_version"),
                        rs.getInt("permission_version")))
                .optional();
    }

    /** 按已验证凭证的账号标识读取该账号的手机号。 */
    @Override
    public Optional<String> findPhoneById(String id) {
        return jdbc.sql("SELECT phone FROM identity_account WHERE id = :id")
                .param("id", Long.parseLong(id))
                .query(String.class)
                .optional();
    }

    /** 按 UTC 写入会话和首个 Refresh Token 哈希，事务由登录用例控制。 */
    @Override
    public void createSession(NewSession session, byte[] tokenHash) {
        // 1. 写入会话及签发时的账号凭证版本。
        var createdAt = LocalDateTime.ofInstant(session.createdAt(), ZoneOffset.UTC);
        var idleExpiresAt = LocalDateTime.ofInstant(session.idleExpiresAt(), ZoneOffset.UTC);
        jdbc.sql("""
                INSERT INTO identity_session
                (id, account_id, client_type, credential_version, created_at, last_used_at,
                    idle_expires_at, absolute_expires_at)
                VALUES (:id, :accountId, :client, :credentialVersion, :createdAt, :createdAt,
                    :idleExpiresAt, :absoluteExpiresAt)
                """)
                .param("id", session.id())
                .param("accountId", Long.parseLong(session.accountId()))
                .param("client", PortalAuthentication.CLIENT)
                .param("credentialVersion", session.credentialVersion())
                .param("createdAt", createdAt)
                .param("idleExpiresAt", idleExpiresAt)
                .param("absoluteExpiresAt", LocalDateTime.ofInstant(session.absoluteExpiresAt(), ZoneOffset.UTC))
                .update();

        // 2. 保存首个凭证哈希，沿用会话的初始空闲到期时间。
        insertRefreshToken(session.id(), tokenHash, createdAt, idleExpiresAt);
    }

    /** 按凭证哈希锁住所属 Portal 会话，刷新和退出共用此锁。 */
    @Override
    public Optional<String> lockSession(byte[] tokenHash) {
        // 先只锁会话行，同一会话下的多个凭证由同一把锁串行处理。
        return jdbc.sql("""
                SELECT id FROM identity_session
                WHERE id = (SELECT session_id FROM identity_refresh_token WHERE token_hash = :hash)
                    AND client_type = 'PORTAL'
                FOR UPDATE
                """)
                .param("hash", tokenHash)
                .query(String.class)
                .optional();
    }

    /** 读取已锁定会话的刷新状态和当前数据库 UTC 时间。 */
    @Override
    public RefreshState refreshState(String sessionId, byte[] tokenHash) {
        // MySQL 的锁定读会看到上一次轮换已提交的消费时间，避免读到等锁前的快照。
        return jdbc.sql("""
                SELECT a.id AS account_id, a.phone, a.status, a.credential_version AS account_version,
                    a.permission_version, s.credential_version AS session_version, s.revoked_at,
                    s.idle_expires_at, s.absolute_expires_at, t.expires_at, t.consumed_at,
                    UTC_TIMESTAMP(3) AS current_utc
                FROM identity_session s
                JOIN identity_account a ON a.id = s.account_id
                JOIN identity_refresh_token t ON t.session_id = s.id
                WHERE s.id = :id AND t.token_hash = :hash
                FOR SHARE OF a, t
                """)
                .param("id", sessionId)
                .param("hash", tokenHash)
                .query((rs, row) -> {
                    var consumedAt = rs.getObject("consumed_at", LocalDateTime.class);
                    return new RefreshState(rs.getString("account_id"), rs.getString("phone"),
                            "ENABLED".equals(rs.getString("status")), rs.getInt("account_version"),
                            rs.getInt("session_version"), rs.getInt("permission_version"),
                            rs.getObject("revoked_at") != null,
                            rs.getObject("current_utc", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                            rs.getObject("idle_expires_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                            rs.getObject("absolute_expires_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                            rs.getObject("expires_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                            consumedAt == null ? null : consumedAt.toInstant(ZoneOffset.UTC));
                })
                .single();
    }

    /** 在呼叫方的事务内执行三次轮换写入。 */
    @Override
    public void rotate(String sessionId, byte[] tokenHash, byte[] successorHash, Instant now, Instant idleExpiresAt) {
        // 1. 消费旧凭证时直接保存数据库 UTC 时间。
        jdbc.sql("""
                UPDATE identity_refresh_token SET consumed_at = UTC_TIMESTAMP(3)
                WHERE token_hash = :hash
                """)
                .param("hash", tokenHash)
                .update();

        // 2. 保存后继凭证的哈希和到期时间。
        var lastUsedAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        var idleDeadline = LocalDateTime.ofInstant(idleExpiresAt, ZoneOffset.UTC);
        insertRefreshToken(sessionId, successorHash, lastUsedAt, idleDeadline);

        // 3. 只更新会话的上次使用时间和空闲期限。
        jdbc.sql("""
                UPDATE identity_session SET last_used_at = :lastUsedAt, idle_expires_at = :idleDeadline
                WHERE id = :id
                """)
                .param("id", sessionId)
                .param("lastUsedAt", lastUsedAt)
                .param("idleDeadline", idleDeadline)
                .update();
    }

    /** 撤销已锁住且仍有效的会话，其所有凭证共用该撤销状态。 */
    @Override
    public boolean revokeActiveSession(String sessionId) {
        return jdbc.sql("""
                UPDATE identity_session SET revoked_at = UTC_TIMESTAMP(3)
                WHERE id = :id AND revoked_at IS NULL
                    AND idle_expires_at > UTC_TIMESTAMP(3) AND absolute_expires_at > UTC_TIMESTAMP(3)
                """)
                .param("id", sessionId)
                .update() == 1;
    }

    /** 通过同一事务顺序记录重放撤销的目标和请求追踪。 */
    @Override
    public void recordReplay(String sessionId, String accountId, String traceId) {
        jdbc.sql("""
                INSERT INTO identity_portal_refresh_replay_audit (session_id, account_id, trace_id, occurred_at)
                VALUES (:sessionId, :accountId, :traceId, UTC_TIMESTAMP(3))
                """)
                .param("sessionId", sessionId)
                .param("accountId", Long.parseLong(accountId))
                .param("traceId", traceId)
                .update();
    }

    /** 只写凭证摘要，登录和轮换共用写入。 */
    private void insertRefreshToken(String sessionId, byte[] tokenHash, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        jdbc.sql("""
                INSERT INTO identity_refresh_token (token_hash, session_id, issued_at, expires_at)
                VALUES (:hash, :sessionId, :issuedAt, :expiresAt)
                """)
                .param("hash", tokenHash)
                .param("sessionId", sessionId)
                .param("issuedAt", issuedAt)
                .param("expiresAt", expiresAt)
                .update();
    }
}
