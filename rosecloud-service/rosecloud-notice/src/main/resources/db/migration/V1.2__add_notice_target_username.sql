ALTER TABLE sys_notice
    ADD COLUMN target_username VARCHAR(128) DEFAULT NULL COMMENT '用户名目标(目标=用户时填)' AFTER target_role_code;
