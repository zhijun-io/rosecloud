ALTER TABLE sys_setting_key
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (id),
    ADD UNIQUE KEY uk_setting_key (setting_key);
