package com.swordshield.next.identity.infrastructure;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import com.swordshield.next.identity.application.PortalAuthentication;
import com.swordshield.next.identity.application.PortalAuthentication.Account;
import com.swordshield.next.identity.application.PortalAuthentication.NewSession;
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
        jdbc.sql("""
                INSERT INTO identity_refresh_token (token_hash, session_id, issued_at, expires_at)
                VALUES (:hash, :sessionId, :issuedAt, :expiresAt)
                """)
                .param("hash", tokenHash)
                .param("sessionId", session.id())
                .param("issuedAt", createdAt)
                .param("expiresAt", idleExpiresAt)
                .update();
    }
}
