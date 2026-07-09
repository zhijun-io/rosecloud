ALTER TABLE sys_notice
    ADD COLUMN recipient_snapshot TEXT DEFAULT NULL COMMENT '推送收件人快照(JSON)' AFTER channels;
