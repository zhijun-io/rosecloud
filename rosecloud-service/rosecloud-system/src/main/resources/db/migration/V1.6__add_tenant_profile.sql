ALTER TABLE sys_tenant
    ADD COLUMN tenant_profile_id VARCHAR(64) DEFAULT NULL COMMENT '租户套餐ID' AFTER remark;

CREATE TABLE IF NOT EXISTS sys_tenant_profile (
  id          VARCHAR(64)  NOT NULL              COMMENT '主键',
  name        VARCHAR(128) NOT NULL              COMMENT '套餐名称',
  description VARCHAR(255) DEFAULT NULL          COMMENT '描述',
  profile_data JSON        DEFAULT NULL          COMMENT '画像数据(JSON)',
  is_default  TINYINT      NOT NULL DEFAULT 0    COMMENT '是否默认:1是 0否',
  create_time DATETIME     DEFAULT NULL          COMMENT '创建时间',
  update_time DATETIME     DEFAULT NULL          COMMENT '更新时间',
  create_by   BIGINT       DEFAULT NULL          COMMENT '创建人',
  update_by   BIGINT       DEFAULT NULL          COMMENT '更新人',
  deleted     TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户套餐表';

INSERT INTO sys_tenant_profile (id, name, description, profile_data, is_default, deleted)
VALUES ('default', '基础版', '系统默认租户套餐',
        JSON_OBJECT('packageCode', 'basic', 'maxUsers', 10, 'maxRoles', 5,
                    'maxNoticesPerDay', 100, 'maxRequestsPerMinute', 60,
                    'enabledCapabilities', JSON_ARRAY()),
        1, 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    profile_data = VALUES(profile_data),
    is_default = VALUES(is_default);
