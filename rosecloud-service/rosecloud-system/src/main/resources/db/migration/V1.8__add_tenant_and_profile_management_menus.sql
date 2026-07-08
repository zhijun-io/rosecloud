-- Tenant direct-create and tenant profile CRUD/default management authorities.

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, status, visible, deleted) VALUES
(53, 6,  '租户新增', 2, NULL, NULL, 'system:tenant:add', NULL, 1, 1, 1, 0),
(54, 6,  '租户修改', 2, NULL, NULL, 'system:tenant:edit', NULL, 2, 1, 1, 0),
(55, 6,  '租户删除', 2, NULL, NULL, 'system:tenant:del', NULL, 3, 1, 1, 0),
(56, 52, '套餐新增', 2, NULL, NULL, 'system:tenant-profile:add', NULL, 1, 1, 1, 0),
(57, 52, '套餐修改', 2, NULL, NULL, 'system:tenant-profile:edit', NULL, 2, 1, 1, 0),
(58, 52, '套餐删除', 2, NULL, NULL, 'system:tenant-profile:del', NULL, 3, 1, 1, 0),
(59, 52, '设为默认', 2, NULL, NULL, 'system:tenant-profile:default', NULL, 4, 1, 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(53, 1, 53),
(54, 1, 54),
(55, 1, 55),
(56, 1, 56),
(57, 1, 57),
(58, 1, 58),
(59, 1, 59)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
