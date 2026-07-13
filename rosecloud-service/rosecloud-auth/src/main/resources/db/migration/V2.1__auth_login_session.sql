-- Auth-owned login session table (authoritative source). Redis keeps the revocation set for fast lookups.
-- See docs/design/auth-system-boundary.md and docs/sdd/2026-07-13-auth-iam-core-spec.md (Slice A).
CREATE TABLE IF NOT EXISTS auth_login_session (
  id            BIGINT       NOT NULL                   COMMENT '主键',
  session_id    VARCHAR(64)  NOT NULL                   COMMENT '会话ID(UUID)',
  user_id       BIGINT       NOT NULL                   COMMENT '用户ID',
  username      VARCHAR(64)  NOT NULL                   COMMENT '用户名',
  nickname      VARCHAR(64)  DEFAULT NULL               COMMENT '昵称',
  token         TEXT         NOT NULL                   COMMENT '访问令牌',
  refresh_token TEXT         DEFAULT NULL               COMMENT '刷新令牌',
  client_ip     VARCHAR(64)  DEFAULT NULL               COMMENT '客户端IP',
  user_agent    VARCHAR(512) DEFAULT NULL               COMMENT '客户端User-Agent',
  device_id     VARCHAR(128) DEFAULT NULL               COMMENT '设备指纹',
  login_at      DATETIME     DEFAULT NULL               COMMENT '登录时间',
  expire_at     DATETIME     DEFAULT NULL               COMMENT '过期时间',
  revoked       TINYINT      NOT NULL DEFAULT 0         COMMENT '是否已吊销:1是 0否',
  create_time   DATETIME     DEFAULT NULL               COMMENT '创建时间',
  update_time   DATETIME     DEFAULT NULL               COMMENT '更新时间',
  create_by     BIGINT       DEFAULT NULL               COMMENT '创建人',
  update_by     BIGINT       DEFAULT NULL               COMMENT '更新人',
  deleted       TINYINT      NOT NULL DEFAULT 0         COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_session_id (session_id),
  KEY idx_user_id (user_id),
  KEY idx_login_at (login_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录会话表(认证服务,权威源)';
