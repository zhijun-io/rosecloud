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

-- Default menu skeleton (platform scope). Types: 0目录 1菜单 2按钮.
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, status, visible, deleted) VALUES
(1,  0, '工作台',   1, '/workbench', 'Workbench',        NULL,                   'dashboard', 1, 1, 1, 0),
(2,  0, '系统管理', 0, '/system',    NULL,                NULL,                   'setting',   2, 1, 1, 0),
(3,  2, '用户管理', 1, 'user',       'system/user/index', 'system:user:list',    'user',      1, 1, 1, 0),
(4,  2, '角色管理', 1, 'role',       'system/role/index', 'system:role:list',    'team',      2, 1, 1, 0),
(5,  2, '菜单管理', 1, 'menu',       'system/menu/index', 'system:menu:list',    'menu',      3, 1, 1, 0),
(6,  2, '租户管理', 1, 'tenant',     'system/tenant/index','system:tenant:list', 'cluster',   4, 1, 1, 0),
(7,  2, '通知中心', 1, 'notice',     'system/notice/index','system:notice:list', 'bell',      5, 1, 1, 0),
(8,  3, '用户新增', 2, NULL, NULL, 'system:user:add',       NULL, 1, 1, 1, 0),
(9,  3, '用户修改', 2, NULL, NULL, 'system:user:edit',      NULL, 2, 1, 1, 0),
(10, 3, '用户删除', 2, NULL, NULL, 'system:user:del',       NULL, 3, 1, 1, 0),
(11, 4, '角色新增', 2, NULL, NULL, 'system:role:add',       NULL, 1, 1, 1, 0),
(12, 4, '角色修改', 2, NULL, NULL, 'system:role:edit',      NULL, 2, 1, 1, 0),
(13, 4, '角色授权', 2, NULL, NULL, 'system:role:perm',      NULL, 3, 1, 1, 0),
(14, 5, '菜单新增', 2, NULL, NULL, 'system:menu:add',       NULL, 1, 1, 1, 0),
(15, 5, '菜单修改', 2, NULL, NULL, 'system:menu:edit',      NULL, 2, 1, 1, 0),
(16, 5, '菜单删除', 2, NULL, NULL, 'system:menu:del',       NULL, 3, 1, 1, 0),
(17, 6, '租户开通', 2, NULL, NULL, 'system:tenant:open',    NULL, 1, 1, 1, 0),
(18, 6, '租户启停', 2, NULL, NULL, 'system:tenant:toggle',  NULL, 2, 1, 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Grant the full skeleton to the platform-admin role (id=1).
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(1,1,1),(2,1,2),(3,1,3),(4,1,4),(5,1,5),(6,1,6),(7,1,7),
(8,1,8),(9,1,9),(10,1,10),(11,1,11),(12,1,12),(13,1,13),
(14,1,14),(15,1,15),(16,1,16),(17,1,17),(18,1,18)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- Tenant-scoped roles (platform-level, shared by all tenants in v1).
INSERT INTO sys_role (id, code, name, deleted) VALUES
(2, 'tenant-admin', '租户管理员', 0),
(3, 'tenant-user', '普通用户', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- tenant-admin: workspace + in-tenant user/role management + notices.
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(19,2,1),(20,2,3),(21,2,4),(22,2,7),
(23,2,8),(24,2,9),(25,2,10),(26,2,11),(27,2,12),(28,2,13)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- tenant-user: workspace + notices.
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(29,3,1),(30,3,7)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- Dictionary management menus (under 系统管理). Platform-admin only.
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, status, visible, deleted) VALUES
(33, 2,  '字典管理', 1, 'dict', 'system/dict/index', 'system:dict:list', 'dict', 7, 1, 1, 0),
(34, 33, '字典新增', 2, NULL, NULL, 'system:dict:add',  NULL, 1, 1, 1, 0),
(35, 33, '字典修改', 2, NULL, NULL, 'system:dict:edit', NULL, 2, 1, 1, 0),
(36, 33, '字典删除', 2, NULL, NULL, 'system:dict:del',  NULL, 3, 1, 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(35, 1, 33), (36, 1, 34), (37, 1, 35), (38, 1, 36)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- Department management menus (under 系统管理). Platform-admin only.
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, status, visible, deleted) VALUES
(37, 2,  '部门管理', 1, 'dept', 'system/dept/index', 'system:dept:list', 'tree', 8, 1, 1, 0),
(38, 37, '部门新增', 2, NULL, NULL, 'system:dept:add',  NULL, 1, 1, 1, 0),
(39, 37, '部门修改', 2, NULL, NULL, 'system:dept:edit', NULL, 2, 1, 1, 0),
(40, 37, '部门删除', 2, NULL, NULL, 'system:dept:del',  NULL, 3, 1, 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(39, 1, 37), (40, 1, 38), (41, 1, 39), (42, 1, 40)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- Root department.
INSERT INTO sys_dept (id, parent_id, name, sort, status, leader, phone, deleted) VALUES
(1, 0, '总公司', 0, 1, NULL, NULL, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Online-user management menu (under 系统管理). Platform-admin only (v1).
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, status, visible, deleted) VALUES
(41, 2, '在线用户', 1, 'online', 'system/online/index', 'system:session:list', 'monitor', 9, 1, 1, 0),
(42, 41, '强制下线', 2, NULL, NULL, 'system:session:kick', NULL, 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(43, 1, 41), (44, 1, 42)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
