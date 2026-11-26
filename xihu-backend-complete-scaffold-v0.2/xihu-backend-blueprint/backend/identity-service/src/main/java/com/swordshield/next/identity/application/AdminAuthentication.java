package com.swordshield.next.identity.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthentication {

    public static final List<String> FIXED_ROLES = List.of("SUPER_ADMIN", "EVENT_OPERATOR", "CHECKIN_STAFF");

    private final Store store;
    private final LoginBudget budget;
    private final Argon2PasswordEncoder passwords;
    private final String dummyHash;

    /** 装配后台登录与在线身份校验用例，预生成恒定开销的虚拟密码哈希。 */
    public AdminAuthentication(Store store, LoginBudget budget, Argon2PasswordEncoder passwords) {
        this.store = store;
        this.budget = budget;
        this.passwords = passwords;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    /**
     * 校验手机号、密码、账号启用状态及固定后台角色准入资格。
     * @return 通过认证与准入后的账号版本与身份角色信息
     */
    public AuthenticatedAdmin login(String phone, String password, String sourceIp) {
        // 1. 验密前检查手机号与来源 IP 两个维度的限流预算。
        if (!budget.allow(phone, sourceIp)) {
            throw new Rejected(Failure.RATE_LIMITED);
        }

        // 2. 手机号不存在时同样执行一次 Argon2id 验证，对外保持统一失败语义。
        AccountWithRoles account = store.findByPhone(phone).orElse(null);
        boolean passwordMatches = passwords.matches(password, account == null ? dummyHash : account.passwordHash());
        if (account == null || !passwordMatches || !account.enabled()) {
            throw new Rejected(Failure.INVALID_CREDENTIALS);
        }

        // 3. 普通启用账号无有效固定后台角色时，拒绝建立后台会话。
        if (account.roles().isEmpty()) {
            throw new Rejected(Failure.ACCESS_REVOKED);
        }

        return new AuthenticatedAdmin(account.credentialVersion(),
                new AdminIdentity(maskProfile(account.id(), phone), account.roles()));
    }

    /**
     * 按 Session 保存的稳定账号标识与登录时凭证版本，实时核对主库当前账号状态及固定角色。
     */
    public AdminIdentity currentIdentity(String accountId, int sessionCredentialVersion) {
        AccountState current = store.findStateById(accountId)
                .orElseThrow(() -> new Rejected(Failure.UNAUTHENTICATED));
        if (!current.enabled() || current.credentialVersion() != sessionCredentialVersion) {
            throw new Rejected(Failure.UNAUTHENTICATED);
        }
        if (current.roles().isEmpty()) {
            throw new Rejected(Failure.ACCESS_REVOKED);
        }
        return new AdminIdentity(maskProfile(accountId, current.phone()), current.roles());
    }

    private static Profile maskProfile(String accountId, String phone) {
        return new Profile(accountId, phone.substring(0, 3) + "****" + phone.substring(7));
    }

    public interface Store {
        Optional<AccountWithRoles> findByPhone(String phone);

        Optional<AccountState> findStateById(String id);
    }

    public interface LoginBudget {
        boolean allow(String phone, String sourceIp);
    }

    public record AccountWithRoles(String id, String passwordHash, boolean enabled, int credentialVersion,
            List<String> roles) {
    }

    public record AccountState(String phone, boolean enabled, int credentialVersion, List<String> roles) {
    }

    public record Profile(String id, String maskedPhone) {
    }

    public record AdminIdentity(Profile user, List<String> roles) {
    }

    public record AuthenticatedAdmin(int credentialVersion, AdminIdentity identity) {
    }

    public enum Failure {
        INVALID_CREDENTIALS,
        UNAUTHENTICATED,
        ACCESS_REVOKED,
        RATE_LIMITED
    }

    public static final class Rejected extends RuntimeException {
        private final Failure failure;

        public Rejected(Failure failure) {
            super(failure.name());
            this.failure = failure;
        }

        public Failure failure() {
            return failure;
        }
    }
}
