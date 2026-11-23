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
    public Login login(String phone, String password, String sourceIp) {
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
        var refreshBytes = new byte[32];
        random.nextBytes(refreshBytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(refreshBytes);
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        String sessionId = UUID.randomUUID().toString();
        var session = new NewSession(sessionId, account.id(), account.credentialVersion(), createdAt,
                createdAt.plus(REFRESH_IDLE), createdAt.plus(SESSION_MAX_AGE));

        // 4. 原子保存会话和凭证哈希。JWT 签发失败时也回滚此次会话写入。
        return transaction.execute(status -> {
            store.createSession(session, hash(refreshToken));
            String accessToken = accessTokens.issue(account, session);
            return new Login(accessToken, refreshToken, profile(account.id(), phone));
        });
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

        void createSession(NewSession session, byte[] tokenHash);
    }

    public interface LoginBudget {
        boolean allow(String phone, String sourceIp);
    }

    public interface AccessTokens {
        String issue(Account account, NewSession session);
    }

    public record Account(String id, String passwordHash, boolean enabled, int credentialVersion, int permissionVersion) {
    }

    public record NewSession(String id, String accountId, int credentialVersion, Instant createdAt,
            Instant idleExpiresAt, Instant absoluteExpiresAt) {
    }

    public record Profile(String id, String maskedPhone) {
    }

    public record Login(String accessToken, String refreshToken, Profile user) {
    }

    public enum Failure { INVALID_CREDENTIALS, RATE_LIMITED }

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
