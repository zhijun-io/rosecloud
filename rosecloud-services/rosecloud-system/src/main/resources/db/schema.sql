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

-- User / role tables. The user store is owned by the system service; the auth
-- service queries it over Feign (see InternalUserController). Column names
-- follow MyBatis-Plus underscore mapping.
CREATE TABLE IF NOT EXISTS sys_user (
  id          BIGINT       NOT NULL                   COMMENT '主键',
  username    VARCHAR(64)  NOT NULL                   COMMENT '用户名',
  password    VARCHAR(128) NOT NULL                   COMMENT '密码(BCrypt)',
  nickname    VARCHAR(64)  DEFAULT NULL               COMMENT '昵称',
  status      TINYINT      NOT NULL DEFAULT 1         COMMENT '状态:1启用 0停用',
  tenant_id   BIGINT       DEFAULT NULL               COMMENT '租户ID(平台账号为空)',
  create_time DATETIME     DEFAULT NULL               COMMENT '创建时间',
  update_time DATETIME     DEFAULT NULL               COMMENT '更新时间',
  create_by   BIGINT       DEFAULT NULL               COMMENT '创建人',
  update_by   BIGINT       DEFAULT NULL               COMMENT '更新人',
  deleted     TINYINT      NOT NULL DEFAULT 0         COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- Platform-level roles (no tenant_id) for v1.
CREATE TABLE IF NOT EXISTS sys_role (
  id          BIGINT       NOT NULL                   COMMENT '主键',
  code        VARCHAR(64)  NOT NULL                   COMMENT '角色编码',
  name        VARCHAR(128) NOT NULL                   COMMENT '角色名',
  create_time DATETIME     DEFAULT NULL               COMMENT '创建时间',
  update_time DATETIME     DEFAULT NULL               COMMENT '更新时间',
  create_by   BIGINT       DEFAULT NULL               COMMENT '创建人',
  update_by   BIGINT       DEFAULT NULL               COMMENT '更新人',
  deleted     TINYINT      NOT NULL DEFAULT 0         COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- User-role link table (pure join, no audit columns).
CREATE TABLE IF NOT EXISTS sys_user_role (
  id       BIGINT NOT NULL              COMMENT '主键',
  user_id  BIGINT NOT NULL              COMMENT '用户ID',
  role_id  BIGINT NOT NULL              COMMENT '角色ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';
