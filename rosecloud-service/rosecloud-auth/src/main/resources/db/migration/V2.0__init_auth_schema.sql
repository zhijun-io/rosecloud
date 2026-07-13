-- Auth-owned login audit log (moved out of rosecloud-system). See docs/design/auth-system-boundary.md.
CREATE TABLE IF NOT EXISTS auth_login_log (
  id          BIGINT       NOT NULL                   COMMENT '主键',
  username    VARCHAR(64)  NOT NULL                   COMMENT '用户名',
  success     TINYINT      NOT NULL                   COMMENT '是否成功:1是 0否',
  fail_reason VARCHAR(255) DEFAULT NULL               COMMENT '失败原因',
  ip          VARCHAR(64)  DEFAULT NULL               COMMENT '登录IP',
  user_agent  VARCHAR(512) DEFAULT NULL               COMMENT '客户端User-Agent',
  login_time  DATETIME     DEFAULT NULL               COMMENT '登录时间',
  create_time DATETIME     DEFAULT NULL               COMMENT '创建时间',
  update_time DATETIME     DEFAULT NULL               COMMENT '更新时间',
  create_by   BIGINT       DEFAULT NULL               COMMENT '创建人',
  update_by   BIGINT       DEFAULT NULL               COMMENT '更新人',
  deleted     TINYINT      NOT NULL DEFAULT 0         COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_username (username),
  KEY idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表(认证服务)';
