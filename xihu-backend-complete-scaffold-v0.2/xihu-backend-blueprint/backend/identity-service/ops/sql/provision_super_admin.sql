-- 1. 在仓库外填写离线生成的 UUIDv7、负责人手机号和 Argon2id 哈希。
-- 使用 mysql 批处理执行本文件。勿使用 -f/--force，以便 SQL 错误中断并关闭连接。
SET SESSION time_zone = '+00:00';

-- 2. 在同一事务内创建账号和授权。
START TRANSACTION;
SET @super_admin_role_id = (SELECT id FROM identity_role WHERE code = 'SUPER_ADMIN');
SET @super_admin_account_id = '<ADMIN_UUID_V7>';
INSERT INTO identity_account (id, phone, password_hash, status)
VALUES (@super_admin_account_id, '<ADMIN_PHONE>', '<ADMIN_ARGON2ID_HASH>', 'ENABLED');

-- 缺少角色时变量为 NULL，由外键列的 NOT NULL 拒绝写入以触发整个脚本中断。
INSERT INTO identity_account_role (account_id, role_id)
VALUES (@super_admin_account_id, @super_admin_role_id);
COMMIT;
