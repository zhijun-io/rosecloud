-- Add device fingerprint to the login audit log (Slice A: audit enhancement).
ALTER TABLE auth_login_log ADD COLUMN device_id VARCHAR(128) DEFAULT NULL COMMENT '设备指纹' AFTER user_agent;
