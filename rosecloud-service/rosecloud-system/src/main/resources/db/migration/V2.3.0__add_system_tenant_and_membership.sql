-- 系统租户（平台管理员所属）与用户-租户成员关系表

CREATE TABLE IF NOT EXISTS sys_user_tenant (
    id         BIGINT       NOT NULL COMMENT '主键(雪花ID)',
    user_id    BIGINT       NOT NULL COMMENT '用户ID',
    tenant_id  VARCHAR(10)   NOT NULL COMMENT '租户ID',
    is_primary TINYINT      NOT NULL DEFAULT 0 COMMENT '是否主租户(0否 1是)',
    create_time DATETIME    DEFAULT NULL,
    update_time DATETIME    DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_tenant (user_id, tenant_id),
    KEY idx_user_tenant_user (user_id),
    KEY idx_user_tenant_tenant (tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户-租户成员关系';

-- 默认系统租户：ROOT，平台管理员所属，保留不可经普通流程变更
INSERT INTO sys_tenant (id, name, status, create_time, update_time, deleted)
SELECT 'ROOT', '系统租户', 1, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_tenant WHERE id = 'ROOT'
);

-- 回填成员关系：每个用户按其归属租户(sys_user.tenant_id)建立主租户成员行
-- 平台管理员(tenant_id 为空)归属系统租户
INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_primary, create_time, update_time)
SELECT
    (UNIX_TIMESTAMP(NOW()) * 1000 + ROW_NUMBER() OVER (ORDER BY u.id)) AS id,
    u.id AS user_id,
    COALESCE(u.tenant_id, 'ROOT') AS tenant_id,
    1 AS is_primary,
    NOW() AS create_time,
    NOW() AS update_time
FROM sys_user u
WHERE u.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_tenant t WHERE t.user_id = u.id AND t.tenant_id = COALESCE(u.tenant_id, 'ROOT')
  );
