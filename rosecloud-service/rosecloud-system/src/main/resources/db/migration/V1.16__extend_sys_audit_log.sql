-- Extend sys_audit_log with entity context, client IP and severity.
ALTER TABLE sys_audit_log
    ADD COLUMN entity_type VARCHAR(64)  DEFAULT NULL COMMENT '操作实体类型(如 user/role/tenant)',
    ADD COLUMN entity_id   VARCHAR(128) DEFAULT NULL COMMENT '操作实体ID',
    ADD COLUMN ip_address  VARCHAR(64)  DEFAULT NULL COMMENT '客户端IP',
    ADD COLUMN severity    VARCHAR(16)  DEFAULT NULL COMMENT '严重级别:INFO/WARN/CRITICAL';

ALTER TABLE sys_audit_log
    ADD KEY idx_entity_type (entity_type),
    ADD KEY idx_severity (severity),
    ADD KEY idx_tenant_id (tenant_id);
