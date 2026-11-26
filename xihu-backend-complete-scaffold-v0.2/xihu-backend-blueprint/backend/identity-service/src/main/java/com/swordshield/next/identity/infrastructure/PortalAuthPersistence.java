package com.swordshield.next.identity.infrastructure;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.swordshield.next.identity.application.PortalAuthentication;
import com.swordshield.next.identity.application.PortalAuthentication.Account;
import com.swordshield.next.identity.application.PortalAuthentication.NewSession;
import com.swordshield.next.identity.application.PortalAuthentication.RefreshState;
import org.springframework.stereotype.Repository;

@Repository
class PortalAuthPersistence implements PortalAuthentication.Store {

    private final IdentityAccountMapper accounts;
    private final PortalSessionMapper sessions;

    PortalAuthPersistence(IdentityAccountMapper accounts, PortalSessionMapper sessions) {
        this.accounts = accounts;
        this.sessions = sessions;
    }

    /** 按唯一手机号读取账号的验密和版本信息。 */
    @Override
    public Optional<Account> findByPhone(String phone) {
        return Optional.ofNullable(accounts.selectOne(
                Wrappers.<IdentityAccount>lambdaQuery().eq(IdentityAccount::getPhone, phone)))
                .map(account -> new Account(account.getId(), account.getPasswordHash(), account.isEnabled(),
                        account.getCredentialVersion(), account.getPermissionVersion()));
    }

    /** 按已验证凭证的账号标识读取手机号。 */
    @Override
    public Optional<String> findPhoneById(String id) {
        return Optional.ofNullable(accounts.selectOne(Wrappers.<IdentityAccount>lambdaQuery()
                .select(IdentityAccount::getPhone).eq(IdentityAccount::getId, id))).map(IdentityAccount::getPhone);
    }

    /** 在登录事务内保存会话及首个刷新凭证摘要，返回新会话 ID。 */
    @Override
    public String createSession(NewSession session, byte[] tokenHash) {
        // 1. 写入会话和签发时的账号凭证版本。
        var row = new IdentitySession(session);
        sessions.insert(row);

        // 2. 首个凭证沿用会话的初始空闲到期时间。
        sessions.insertRefreshToken(row.getId(), tokenHash, row.getCreatedAt(), row.getIdleExpiresAt());
        return row.getId();
    }

    /** 根据凭证摘要锁定并返回所属 Portal 会话 ID。 */
    @Override
    public Optional<String> lockSession(byte[] tokenHash) {
        return sessions.lockSession(tokenHash);
    }

    /** 读取已锁定会话的刷新状态，数据库时间按 UTC 转换。 */
    @Override
    public RefreshState refreshState(String sessionId, byte[] tokenHash) {
        var state = sessions.selectRefreshState(sessionId, tokenHash);
        return new RefreshState(state.accountId(), state.phone(), state.enabled(), state.accountVersion(),
                state.sessionVersion(), state.permissionVersion(), state.revoked(), state.now().toInstant(ZoneOffset.UTC),
                state.idleExpiresAt().toInstant(ZoneOffset.UTC), state.absoluteExpiresAt().toInstant(ZoneOffset.UTC),
                state.tokenExpiresAt().toInstant(ZoneOffset.UTC),
                state.consumedAt() == null ? null : state.consumedAt().toInstant(ZoneOffset.UTC));
    }

    /** 在同一事务内消费旧凭证、写入后继凭证并推进会话的空闲期限。 */
    @Override
    public void rotate(String sessionId, byte[] tokenHash, byte[] successorHash, Instant now, Instant idleExpiresAt) {
        // 1. 以数据库 UTC 时间标记旧凭证已消费。
        sessions.consumeRefreshToken(tokenHash);

        // 2. 保存后继凭证并更新会话的上次使用时间和空闲期限。
        var lastUsedAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        var idleDeadline = LocalDateTime.ofInstant(idleExpiresAt, ZoneOffset.UTC);
        sessions.insertRefreshToken(sessionId, successorHash, lastUsedAt, idleDeadline);
        sessions.updateLastUse(sessionId, lastUsedAt, idleDeadline);
    }

    /** 撤销已加锁且仍有效的会话，返回本次是否实际撤销。 */
    @Override
    public boolean revokeActiveSession(String sessionId) {
        return sessions.revokeActiveSession(sessionId) == 1;
    }

    /** 在撤销事务内记录重放目标与请求追踪标识。 */
    @Override
    public void recordReplay(String sessionId, String accountId, String traceId) {
        sessions.insertReplayAudit(sessionId, accountId, traceId);
    }
}
