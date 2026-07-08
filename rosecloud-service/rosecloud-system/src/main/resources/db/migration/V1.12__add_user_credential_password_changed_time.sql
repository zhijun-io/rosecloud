ALTER TABLE user_credential
    ADD COLUMN IF NOT EXISTS password_changed_time DATETIME DEFAULT NULL COMMENT '密码更新时间' AFTER password;
