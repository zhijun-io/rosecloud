-- RoseCloud auth: user table. Apply manually (or via a migration tool) to the
-- service database. Column names follow MyBatis-Plus underscore mapping.
CREATE TABLE IF NOT EXISTS sys_user (
  id          BIGINT       NOT NULL                   COMMENT '主键',
  username    VARCHAR(64)  NOT NULL                   COMMENT '用户名',
  password    VARCHAR(128) NOT NULL                   COMMENT '密码(BCrypt)',
  nickname    VARCHAR(64)  DEFAULT NULL               COMMENT '昵称',
  status      TINYINT      NOT NULL DEFAULT 1         COMMENT '状态:1启用 0停用',
  tenant_id   BIGINT       DEFAULT NULL               COMMENT '租户ID',
  create_time DATETIME     DEFAULT NULL               COMMENT '创建时间',
  update_time DATETIME     DEFAULT NULL               COMMENT '更新时间',
  create_by   BIGINT       DEFAULT NULL               COMMENT '创建人',
  update_by   BIGINT       DEFAULT NULL               COMMENT '更新人',
  deleted     TINYINT      NOT NULL DEFAULT 0         COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
