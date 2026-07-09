-- Tenant profile lookup menu and authority.
-- Tenant profiles are platform-owned and read-only in v1; platform-admin gets the entry.

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, status, visible, deleted) VALUES
(52, 2, '租户套餐', 1, 'tenant-profile', 'system/tenant-profile/index', 'system:tenant-profile:list', 'box', 5, 1, 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(52, 1, 52)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
