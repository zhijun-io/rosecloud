-- Tenant management button permissions for platform admins.
-- V1.8 reused menu ids 53-55, which already belong to V1.4 setting-key buttons.
-- Use fresh ids here so platform-admin actually receives tenant CRUD authorities.

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, status, visible, deleted) VALUES
(60, 6, '租户新增', 2, NULL, NULL, 'system:tenant:add', NULL, 1, 1, 1, 0),
(61, 6, '租户修改', 2, NULL, NULL, 'system:tenant:edit', NULL, 2, 1, 1, 0),
(62, 6, '租户删除', 2, NULL, NULL, 'system:tenant:del', NULL, 3, 1, 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(60, 1, 60),
(61, 1, 61),
(62, 1, 62)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
