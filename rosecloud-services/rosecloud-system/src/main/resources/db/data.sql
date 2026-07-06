-- Seed platform admin. Password: admin123 (BCrypt). Change after first login.
-- Role codes flow into the JWT `roles` claim and the gateway X-Roles header.
INSERT INTO sys_role (id, code, name, deleted)
VALUES (1, 'platform-admin', '平台管理员', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_user (id, username, password, nickname, status, tenant_id, deleted)
VALUES (1, 'admin', '$2a$10$ipYqBLPr/rGe5c1AVgvWoODGrthzi8FKOjGE7HZQQ0EATEPaY/OJa', '平台管理员', 1, NULL, 0)
ON DUPLICATE KEY UPDATE password = VALUES(password), status = 1;

INSERT INTO sys_user_role (id, user_id, role_id)
VALUES (1, 1, 1)
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);
