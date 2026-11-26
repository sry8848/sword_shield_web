-- 1. 为 dev 和 test 补充会务运营与现场核验测试账号，凭证仅供本地测试。
INSERT INTO identity_account (id, phone, password_hash, status)
VALUES ('01935f3a-7b2c-7abc-8def-000000000014', '13800000004', '$argon2id$v=19$m=19456,t=2,p=1$M+xGRW2R28r9lw5L+0yfEQ$oxlUvs4VTAl0KkkwRT3cFRFZ6Cy60AQAd+RcZ71rRCg', 'ENABLED'),
       ('01935f3a-7b2c-7abc-8def-000000000015', '13800000005', '$argon2id$v=19$m=19456,t=2,p=1$gK1hHMvXyqSYxwmQfnZG3A$67/FIGwhMW691SeuZG1ZMgq97FSTlz21rRIMAJD3tPc', 'ENABLED');

-- 2. 分别授予 EVENT_OPERATOR 与 CHECKIN_STAFF 固定角色。
SET @test_operator_account_id = (SELECT id FROM identity_account WHERE phone = '13800000004');
SET @test_operator_role_id = (SELECT id FROM identity_role WHERE code = 'EVENT_OPERATOR');
INSERT INTO identity_account_role (account_id, role_id)
VALUES (@test_operator_account_id, @test_operator_role_id);

SET @test_checkin_account_id = (SELECT id FROM identity_account WHERE phone = '13800000005');
SET @test_checkin_role_id = (SELECT id FROM identity_role WHERE code = 'CHECKIN_STAFF');
INSERT INTO identity_account_role (account_id, role_id)
VALUES (@test_checkin_account_id, @test_checkin_role_id);
