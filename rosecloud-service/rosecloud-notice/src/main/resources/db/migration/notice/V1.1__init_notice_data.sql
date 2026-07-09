-- Seed a global welcome notice (already published).
INSERT INTO sys_notice (id, title, content, target_type, target_tenant_id, target_role_code, target_username,
                        publish_type, publish_time, effective_time, expire_time, status, need_confirm,
                        sender_id, tenant_id, deleted)
VALUES (1, '欢迎接入 RoseCloud', '平台已就绪，请各租户管理员完成初始化配置。', 0, NULL, NULL, NULL,
        0, NOW(), NULL, NULL, 1, 0, NULL, NULL, 0)
ON DUPLICATE KEY UPDATE title = VALUES(title);
