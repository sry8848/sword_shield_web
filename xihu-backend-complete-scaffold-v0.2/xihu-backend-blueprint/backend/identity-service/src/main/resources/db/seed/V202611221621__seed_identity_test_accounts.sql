-- 1. 为 dev 和 test 建立普通账号和后台账号，凭证仅供本地测试。
INSERT INTO identity_account (id, phone, password_hash, status)
VALUES ('01935f3a-7b2c-7abc-8def-000000000011', '13800000001', '$argon2id$v=19$m=19456,t=2,p=1$PnW1gkuaZNfGFm560AUayg$lKh+EKI8O361J5RB5vo0PAPRlUkn7SFW52FvOpVe070', 'ENABLED'),
       ('01935f3a-7b2c-7abc-8def-000000000012', '13800000002', '$argon2id$v=19$m=19456,t=2,p=1$r7Bjve8THQnn7IdFLbii8w$fgSfYQUbFtuCUJQ3PGzTWmC0W8J5RaJEF27fnvdvtq0', 'ENABLED');

-- 2. 只为测试管理员授予后台角色。
SET @test_admin_account_id = (SELECT id FROM identity_account WHERE phone = '13800000002');
SET @test_admin_role_id = (SELECT id FROM identity_role WHERE code = 'SUPER_ADMIN');
INSERT INTO identity_account_role (account_id, role_id)
VALUES (@test_admin_account_id, @test_admin_role_id);
