-- Auth-owned credential table. Password hashes live here, not in the system service.
-- See docs/design/auth-system-boundary.md and docs/sdd/2026-07-13-auth-iam-core-spec.md (Slice B).
CREATE TABLE IF NOT EXISTS auth_credential (
  id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id             BIGINT       NOT NULL                COMMENT '用户ID',
  password_hash       VARCHAR(100) NOT NULL                COMMENT 'BCrypt密码哈希',
  password_changed_time DATETIME   DEFAULT NULL            COMMENT '密码修改时间',
  auth_status         TINYINT      NOT NULL DEFAULT 1       COMMENT '认证状态:1启用 0禁用',
  last_login_time     DATETIME     DEFAULT NULL            COMMENT '最后登录时间',
  create_time         DATETIME     DEFAULT NULL            COMMENT '创建时间',
  update_time         DATETIME     DEFAULT NULL            COMMENT '更新时间',
  create_by           BIGINT       DEFAULT NULL            COMMENT '创建人',
  update_by           BIGINT       DEFAULT NULL            COMMENT '更新人',
  deleted             TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_id (user_id),
  KEY idx_last_login_time (last_login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户凭证表(认证服务)';

-- Seed platform admin credential. Password: admin123 (BCrypt, matches system seed sys_user id=1).
-- Kept in sync with rosecloud-system V1.1 initial data so the seeded admin can log in after
-- credentials move to auth.
INSERT INTO auth_credential (id, user_id, password_hash, password_changed_time, auth_status, deleted)
VALUES (1, 1, '$2a$10$ipYqBLPr/rGe5c1AVgvWoODGrthzi8FKOjGE7HZQQ0EATEPaY/OJa', CURRENT_TIMESTAMP, 1, 0)
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), auth_status = VALUES(auth_status), deleted = 0;
