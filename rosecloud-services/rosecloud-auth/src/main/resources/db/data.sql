-- Seed platform admin. Password: admin123 (BCrypt). Change after first login.
INSERT INTO sys_user (id, username, password, nickname, status, tenant_id, deleted)
VALUES (1, 'admin', '$2a$10$ipYqBLPr/rGe5c1AVgvWoODGrthzi8FKOjGE7HZQQ0EATEPaY/OJa', '平台管理员', 1, NULL, 0)
ON DUPLICATE KEY UPDATE password = VALUES(password), status = 1;
