-- 1. 在仓库外填写项目负责人指定的手机号和离线生成的 Argon2id 哈希。
-- 使用 mysql 批处理执行本文件。勿使用 -f/--force，以便 SQL 错误中断并关闭连接。
SET SESSION time_zone = '+00:00';

-- 2. 在同一事务内创建账号和授权。
START TRANSACTION;
SET @super_admin_role_id = (SELECT id FROM identity_role WHERE code = 'SUPER_ADMIN');
INSERT INTO identity_account (phone, password_hash, status)
VALUES ('<ADMIN_PHONE>', '<ADMIN_ARGON2ID_HASH>', 'ENABLED');

-- 缺少角色时变量为 NULL，由外键列的 NOT NULL 拒绝写入以触发整个脚本中断。
INSERT INTO identity_account_role (account_id, role_id)
VALUES (LAST_INSERT_ID(), @super_admin_role_id);
COMMIT;
