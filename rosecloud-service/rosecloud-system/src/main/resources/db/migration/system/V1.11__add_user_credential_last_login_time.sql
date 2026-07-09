 ALTER TABLE user_credential
     ADD COLUMN last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间' AFTER send_time;
