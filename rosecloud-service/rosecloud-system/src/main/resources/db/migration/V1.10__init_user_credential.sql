-- Backward-compatible credential migration for older databases that still keep
-- the password column on sys_user. Fresh installs already create user_credential
-- in V1.0 and seed it in V1.1, so these statements become no-ops there.
SET @has_password_column := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'password'
);

SET @sql := IF(
  @has_password_column > 0,
  'INSERT INTO user_credential (id, user_id, password, password_changed_time, deleted)
   SELECT id, id, password, CURRENT_TIMESTAMP, 0
   FROM sys_user
   WHERE password IS NOT NULL
   ON DUPLICATE KEY UPDATE password = VALUES(password), password_changed_time = VALUES(password_changed_time), deleted = 0',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  @has_password_column > 0,
  'ALTER TABLE sys_user DROP COLUMN password',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
