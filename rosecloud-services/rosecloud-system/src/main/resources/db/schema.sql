-- RoseCloud system: tenant table. Apply manually (or via a migration tool).
CREATE TABLE IF NOT EXISTS sys_tenant (
  id            BIGINT       NOT NULL              COMMENT '主键',
  name          VARCHAR(128) NOT NULL              COMMENT '租户名',
  code          VARCHAR(64)  NOT NULL              COMMENT '租户编码',
  status        TINYINT      NOT NULL DEFAULT 0    COMMENT '状态:0待开通 1启用 2停用',
  contact_user  VARCHAR(64)  DEFAULT NULL          COMMENT '联系人',
  contact_phone VARCHAR(32)  DEFAULT NULL          COMMENT '联系电话',
  expire_time   DATE         DEFAULT NULL          COMMENT '到期时间',
  remark        VARCHAR(255) DEFAULT NULL          COMMENT '备注',
  create_time   DATETIME     DEFAULT NULL          COMMENT '创建时间',
  update_time   DATETIME     DEFAULT NULL          COMMENT '更新时间',
  create_by     BIGINT       DEFAULT NULL          COMMENT '创建人',
  update_by     BIGINT       DEFAULT NULL          COMMENT '更新人',
  deleted       TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';
