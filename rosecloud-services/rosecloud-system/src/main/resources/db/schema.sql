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
  admin_username VARCHAR(64)  DEFAULT NULL          COMMENT '首个管理员用户名',
  admin_password VARCHAR(128) DEFAULT NULL          COMMENT '首个管理员密码(BCrypt,开通后清空)',
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

-- Menu / permission tables. Menus drive both navigation and permission codes
-- (button-typed menus carry a `perms` code). Role-menu binding controls access.
CREATE TABLE IF NOT EXISTS sys_menu (
  id          BIGINT       NOT NULL                   COMMENT '主键',
  parent_id   BIGINT       NOT NULL DEFAULT 0         COMMENT '父菜单ID(0=根)',
  name        VARCHAR(64)  NOT NULL                   COMMENT '菜单名称',
  type        TINYINT      NOT NULL                   COMMENT '类型:0目录 1菜单 2按钮',
  path        VARCHAR(128) DEFAULT NULL               COMMENT '路由路径',
  component   VARCHAR(128) DEFAULT NULL               COMMENT '前端组件',
  perms       VARCHAR(128) DEFAULT NULL               COMMENT '权限标识(如 system:user:add)',
  icon        VARCHAR(64)  DEFAULT NULL               COMMENT '图标',
  sort        INT          NOT NULL DEFAULT 0         COMMENT '排序',
  status      TINYINT      NOT NULL DEFAULT 1         COMMENT '状态:1启用 0停用',
  visible     TINYINT      NOT NULL DEFAULT 1         COMMENT '是否可见:1是 0否',
  create_time DATETIME     DEFAULT NULL               COMMENT '创建时间',
  update_time DATETIME     DEFAULT NULL               COMMENT '更新时间',
  create_by   BIGINT       DEFAULT NULL               COMMENT '创建人',
  update_by   BIGINT       DEFAULT NULL               COMMENT '更新人',
  deleted     TINYINT      NOT NULL DEFAULT 0         COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
  id      BIGINT NOT NULL              COMMENT '主键',
  role_id BIGINT NOT NULL              COMMENT '角色ID',
  menu_id BIGINT NOT NULL              COMMENT '菜单ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- Operation audit log. Populated by AuditLogPersistenceListener from AuditLogEvent.
CREATE TABLE IF NOT EXISTS sys_audit_log (
  id             BIGINT       NOT NULL                   COMMENT '主键',
  action         VARCHAR(64)  NOT NULL                   COMMENT '操作动作',
  description    VARCHAR(255) DEFAULT NULL               COMMENT '描述',
  principal      VARCHAR(64)  DEFAULT NULL               COMMENT '操作人',
  tenant_id      BIGINT       DEFAULT NULL               COMMENT '租户ID',
  target         VARCHAR(128) DEFAULT NULL               COMMENT '目标方法',
  elapsed_millis BIGINT       DEFAULT NULL               COMMENT '耗时(毫秒)',
  success        TINYINT      NOT NULL                   COMMENT '是否成功:1是 0否',
  error          VARCHAR(255) DEFAULT NULL               COMMENT '失败信息',
  create_time    DATETIME     DEFAULT NULL               COMMENT '创建时间',
  update_time    DATETIME     DEFAULT NULL               COMMENT '更新时间',
  create_by      BIGINT       DEFAULT NULL               COMMENT '创建人',
  update_by      BIGINT       DEFAULT NULL               COMMENT '更新人',
  deleted        TINYINT      NOT NULL DEFAULT 0         COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_create_time (create_time),
  KEY idx_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';
