package com.swordshield.next.identity;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "APP_VERSION=identity-test")
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "IDENTITY_TEST_DB_URL", matches = ".+")
class IdentityAccountDataTest {

    private static final String PORTAL_PHONE = "13800000001";
    private static final String ADMIN_PHONE = "13800000002";
    private static final String TEST_PHONE = "13800000003";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Flyway flyway;

    @Autowired
    private Argon2PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void signingKey(DynamicPropertyRegistry registry) {
        PortalTestSigningKey.configure(registry);
    }

    @Test
    void testProfileMigratesAndSeedsBothAccountsOnce() {
        // 1. 检查测试账号、初始角色、凭证版本和连接时区。
        assertThat(jdbc.queryForList(
                "SELECT status FROM identity_account WHERE phone IN (?, ?) ORDER BY phone", String.class,
                PORTAL_PHONE, ADMIN_PHONE)).containsExactly("ENABLED", "ENABLED");
        assertThat(rolesOf(PORTAL_PHONE)).isEmpty();
        assertThat(rolesOf(ADMIN_PHONE)).containsExactly("SUPER_ADMIN");
        assertThat(jdbc.queryForList("""
                SELECT credential_version + permission_version
                FROM identity_account WHERE phone IN (?, ?)
                """, Integer.class, PORTAL_PHONE, ADMIN_PHONE)).containsExactly(2, 2);
        assertThat(jdbc.queryForObject("SELECT @@session.time_zone", String.class)).isEqualTo("+00:00");

        // 2. 用生产编码器核对测试凭证并验证重新迁移不会覆盖种子。
        String portalHash = hashOf(PORTAL_PHONE);
        String adminHash = hashOf(ADMIN_PHONE);
        assertThat(passwordEncoder.matches("LocalPortal-Only!2026", portalHash)).isTrue();
        assertThat(passwordEncoder.matches("LocalAdmin-Only!2026", adminHash)).isTrue();
        assertThat(passwordEncoder.matches("wrong-password", adminHash)).isFalse();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(hashOf(PORTAL_PHONE)).isEqualTo(portalHash);
        assertThat(hashOf(ADMIN_PHONE)).isEqualTo(adminHash);
    }

    @Test
    @Transactional
    void rejectsInvalidAccountAndRoleData() {
        // 1. 直接写真实表，确认数据库约束也适用于脚本写入。
        String hash = hashOf(PORTAL_PHONE);
        assertThatThrownBy(() -> insertAccount(PORTAL_PHONE, hash)).isInstanceOf(DuplicateKeyException.class);
        assertCheckRejected(() -> insertAccount("12800000003", hash));
        assertCheckRejected(() -> insertAccount(TEST_PHONE, "plaintext"));
        assertCheckRejected(() -> insertAccount(TEST_PHONE, ""));
        for (String invalidCode : new String[]{"", " ", "super_admin", "SUPER_ADMIN ", "OPS\n"}) {
            assertCheckRejected(() -> jdbc.update("INSERT INTO identity_role (code) VALUES (?)", invalidCode));
        }
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO identity_account_role (account_id, role_id)
                SELECT account_id, role_id FROM identity_account_role
                """)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @Transactional
    void recordsCreationAndUpdateTimesInUtc() {
        // 1. 写入一个时间明确的账号，避免测试依赖睡眠和数据库时钟精度。
        LocalDateTime originalTime = LocalDateTime.of(2020, 1, 1, 0, 0);
        jdbc.update("""
                INSERT INTO identity_account (phone, password_hash, status, created_at, updated_at)
                VALUES (?, ?, 'ENABLED', ?, ?)
                """, TEST_PHONE, hashOf(PORTAL_PHONE), originalTime, originalTime);

        // 2. 更新账号后，创建时间保留，更新时间由数据库生成。
        jdbc.update("UPDATE identity_account SET status = 'DISABLED' WHERE phone = ?", TEST_PHONE);
        assertThat(jdbc.queryForObject("SELECT created_at FROM identity_account WHERE phone = ?",
                LocalDateTime.class, TEST_PHONE)).isEqualTo(originalTime);
        LocalDateTime databaseNow = jdbc.queryForObject("SELECT UTC_TIMESTAMP(3)", LocalDateTime.class);
        assertThat(jdbc.queryForObject("SELECT updated_at FROM identity_account WHERE phone = ?",
                LocalDateTime.class, TEST_PHONE)).isBetween(databaseNow.minusSeconds(10), databaseNow);
    }

    /** 验证指定写入触发 MySQL 的 CHECK 约束错误。 */
    private void assertCheckRejected(ThrowingCallable write) {
        // Connector/J 将 CHECK 违例归为 HY000，直接核对 MySQL 错误码。
        assertThatThrownBy(write).rootCause().isInstanceOfSatisfying(SQLException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(3819));
    }

    /** 查询指定测试手机号对应的后台角色编码。 */
    private List<String> rolesOf(String phone) {
        return jdbc.queryForList("""
                SELECT r.code FROM identity_account_role ar
                JOIN identity_account a ON a.id = ar.account_id
                JOIN identity_role r ON r.id = ar.role_id
                WHERE a.phone = ?
                """, String.class, phone);
    }

    /** 读取指定种子账号的密码哈希供约束和兼容性测试使用。 */
    private String hashOf(String phone) {
        return jdbc.queryForObject("SELECT password_hash FROM identity_account WHERE phone = ?", String.class, phone);
    }

    /** 用给定手机号和哈希尝试插入启用账号。 */
    private void insertAccount(String phone, String hash) {
        jdbc.update("INSERT INTO identity_account (phone, password_hash, status) VALUES (?, ?, 'ENABLED')", phone, hash);
    }
}
