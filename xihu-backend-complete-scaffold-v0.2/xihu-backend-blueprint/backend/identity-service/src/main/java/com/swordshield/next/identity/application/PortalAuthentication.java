package com.swordshield.next.identity.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PortalAuthentication {

    public static final String CLIENT = "PORTAL";
    public static final int ACCESS_SECONDS = 600;
    public static final Duration REFRESH_IDLE = Duration.ofDays(7);
    private static final Duration REFRESH_GRACE = Duration.ofSeconds(10);
    private static final Duration SESSION_MAX_AGE = Duration.ofDays(30);

    private final Store store;
    private final LoginBudget budget;
    private final AccessTokens accessTokens;
    private final Argon2PasswordEncoder passwords;
    private final TransactionTemplate transaction;
    private final SecureRandom random = new SecureRandom();
    private final String dummyHash;

    /** 装配登录用例，并预先生成与真实账号同成本的虚拟哈希。 */
    public PortalAuthentication(Store store, LoginBudget budget, AccessTokens accessTokens,
            Argon2PasswordEncoder passwords, PlatformTransactionManager transactionManager) {
        this.store = store;
        this.budget = budget;
        this.accessTokens = accessTokens;
        this.passwords = passwords;
        this.transaction = new TransactionTemplate(transactionManager);
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    /**
     * 校验已通过格式校验的手机号、密码和来源地址，建立 Portal 会话。
     * @return 已提交会话的身份摘要和两类凭证
     */
    public SessionTokens login(String phone, String password, String sourceIp) {
        // 1. 验密前检查两个维度的预算。
        if (!budget.allow(phone, sourceIp)) {
            throw new Rejected(Failure.RATE_LIMITED);
        }

        // 2. 未找到账号时也执行一次 Argon2id 验证，对外保持同一失败语义。
        Account account = store.findByPhone(phone).orElse(null);
        boolean passwordMatches = passwords.matches(password, account == null ? dummyHash : account.passwordHash());
        if (account == null || !passwordMatches || !account.enabled()) {
            throw new Rejected(Failure.INVALID_CREDENTIALS);
        }

        // 3. 生成首个续期凭证和两项会话期限。
        String refreshToken = newRefreshToken();
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        var session = new NewSession(account.id(), account.credentialVersion(), createdAt,
                createdAt.plus(REFRESH_IDLE), createdAt.plus(SESSION_MAX_AGE));

        // 4. 原子保存会话和凭证哈希。JWT 签发失败时也回滚此次会话写入。
        return transaction.execute(status -> {
            String sessionId = store.createSession(session, hash(refreshToken));
            String accessToken = accessTokens.issue(sessionId, account.id(), account.credentialVersion(),
                    account.permissionVersion(), createdAt);
            return new SessionTokens(accessToken, refreshToken, REFRESH_IDLE, profile(account.id(), phone));
        });
    }

    /** 在会话锁内处理刷新凭证，提交后才返回刷新结果。 */
    public SessionTokens refresh(String refreshToken, String traceId) {
        byte[] tokenHash = hash(refreshToken);
        var refreshed = transaction.execute(status -> refreshInTransaction(tokenHash, traceId));

        // 重放的 401 在事务外返回，确保撤销和审计已提交。
        return refreshed.orElseThrow(() -> new Rejected(Failure.REFRESH_REPLAYED));
    }

    /** 解析已锁定会话的凭证，并且只在首次消费时保存后继凭证。 */
    private Optional<SessionTokens> refreshInTransaction(byte[] tokenHash, String traceId) {
        // 1. 先锁住所属会话，再读取最新的凭证和账号状态。
        String sessionId = store.lockSession(tokenHash).orElseThrow(() -> new Rejected(Failure.INVALID_REFRESH));
        RefreshState session = store.refreshState(sessionId, tokenHash);
        Instant now = session.now();
        if (session.revoked() || !session.enabled()
                || session.sessionCredentialVersion() != session.accountCredentialVersion()) {
            throw new Rejected(Failure.INVALID_REFRESH);
        }
        if (!now.isBefore(session.idleExpiresAt()) || !now.isBefore(session.absoluteExpiresAt())) {
            throw new Rejected(Failure.REFRESH_EXPIRED);
        }

        // 2. 已消费的凭证只在宽限期内签发 Access Token。
        if (session.consumedAt() != null) {
            if (withinRefreshGrace(session.consumedAt(), now)) {
                return Optional.of(issueRefresh(sessionId, session, null, Duration.ZERO));
            }
            if (!store.revokeActiveSession(sessionId)) {
                throw new Rejected(Failure.REFRESH_EXPIRED);
            }
            store.recordReplay(sessionId, session.accountId(), traceId);
            return Optional.empty();
        }

        // 3. 消费旧凭证、保存唯一后继，并延长会话的空闲期限。
        if (!now.isBefore(session.tokenExpiresAt())) {
            throw new Rejected(Failure.REFRESH_EXPIRED);
        }
        String successor = newRefreshToken();
        Instant idleExpiresAt = now.plus(REFRESH_IDLE);
        if (idleExpiresAt.isAfter(session.absoluteExpiresAt())) {
            idleExpiresAt = session.absoluteExpiresAt();
        }
        store.rotate(sessionId, tokenHash, hash(successor), now, idleExpiresAt);
        return Optional.of(issueRefresh(sessionId, session, successor, Duration.between(now, idleExpiresAt)));
    }

    /** 宽限期从旧凭证的 UTC 消费时间起算，含恰好 10 秒。 */
    static boolean withinRefreshGrace(Instant consumedAt, Instant now) {
        return !now.isAfter(consumedAt.plus(REFRESH_GRACE));
    }

    /** 撤销凭证所属的单个 Portal 会话，已失效的会话重复退出也成功。 */
    public void logout(String refreshToken) {
        byte[] tokenHash = hash(refreshToken);
        transaction.executeWithoutResult(status -> store.lockSession(tokenHash)
                .ifPresent(store::revokeActiveSession));
    }

    /** 签发新的 Access Token，必要时在结果中附带后继 Refresh Token。 */
    private SessionTokens issueRefresh(String sessionId, RefreshState session, String successor, Duration cookieMaxAge) {
        String accessToken = accessTokens.issue(sessionId, session.accountId(), session.accountCredentialVersion(),
                session.permissionVersion(), Instant.now());
        return new SessionTokens(accessToken, successor, cookieMaxAge, profile(session.accountId(), session.phone()));
    }

    /** 生成适合 Cookie 传输的 256 位随机凭证。 */
    private String newRefreshToken() {
        var refreshBytes = new byte[32];
        random.nextBytes(refreshBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(refreshBytes);
    }

    /** 从已验签的账号标识查询身份摘要。 */
    public Profile currentProfile(String accountId) {
        String phone = store.findPhoneById(accountId).orElseThrow(() -> new Rejected(Failure.INVALID_CREDENTIALS));
        return profile(accountId, phone);
    }

    /** 保留手机号前三位和后四位用于身份展示。 */
    private static Profile profile(String accountId, String phone) {
        return new Profile(accountId, phone.substring(0, 3) + "****" + phone.substring(7));
    }

    /** 对浏览器实际持有的令牌字符串执行 SHA-256，数据库只接收摘要。 */
    private static byte[] hash(String refreshToken) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(refreshToken.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JVM 不支持 SHA-256", ex);
        }
    }

    public interface Store {
        Optional<Account> findByPhone(String phone);

        Optional<String> findPhoneById(String id);

        String createSession(NewSession session, byte[] tokenHash);

        Optional<String> lockSession(byte[] tokenHash);

        RefreshState refreshState(String sessionId, byte[] tokenHash);

        void rotate(String sessionId, byte[] tokenHash, byte[] successorHash, Instant now, Instant idleExpiresAt);

        boolean revokeActiveSession(String sessionId);

        void recordReplay(String sessionId, String accountId, String traceId);
    }

    public interface LoginBudget {
        boolean allow(String phone, String sourceIp);
    }

    public interface AccessTokens {
        String issue(String sessionId, String accountId, int credentialVersion, int permissionVersion, Instant issuedAt);
    }

    public record Account(String id, String passwordHash, boolean enabled, int credentialVersion, int permissionVersion) {
    }

    public record NewSession(String accountId, int credentialVersion, Instant createdAt,
            Instant idleExpiresAt, Instant absoluteExpiresAt) {
    }

    public record Profile(String id, String maskedPhone) {
    }

    public record SessionTokens(String accessToken, String refreshToken, Duration cookieMaxAge, Profile user) {
    }

    public record RefreshState(String accountId, String phone, boolean enabled, int accountCredentialVersion,
            int sessionCredentialVersion, int permissionVersion, boolean revoked, Instant now,
            Instant idleExpiresAt, Instant absoluteExpiresAt, Instant tokenExpiresAt, Instant consumedAt) {
    }

    public enum Failure { INVALID_CREDENTIALS, RATE_LIMITED, INVALID_REFRESH, REFRESH_EXPIRED, REFRESH_REPLAYED }

    public static final class Rejected extends RuntimeException {
        private final Failure failure;

        Rejected(Failure failure) {
            super(failure.name());
            this.failure = failure;
        }

        public Failure failure() {
            return failure;
        }
    }
}
