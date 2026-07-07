-- Fine-grained button-level permission codes for the remaining coarse role-scoped
-- endpoints (notice publish, config CRUD, audit log, login log). Each becomes a
-- dedicated sys_menu button/menu row with a `perms` code; roles are linked via
-- sys_role_menu. The JWT `perms` claim is aggregated from these rows automatically
-- by UserRepositoryImpl.loadPerms (user_role -> role_menu -> menu.perms).
--
-- NOTE: a stale duplicate of V1.2 (add_login_log_ip_user_agent) once existed as a
-- V1.3 artifact under target/; it is identical to V1.2 and has been removed so this
-- migration is the sole V1.3 in the chain.

-- Notification publish button (under 通知中心, menu id 7).
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, status, visible, deleted) VALUES
(43, 7,  '通知发布', 2, NULL, NULL, 'system:notice:publish', NULL, 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Config management (under 系统管理, menu id 2): menu + add/edit/del buttons.
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, status, visible, deleted) VALUES
(44, 2,  '配置管理', 1, 'config',     'system/config/index',  'system:config:list', 'setting', 10, 1, 1, 0),
(45, 44, '配置新增', 2, NULL,         NULL,                    'system:config:add',  NULL,      1, 1, 1, 0),
(46, 44, '配置修改', 2, NULL,         NULL,                    'system:config:edit', NULL,      2, 1, 1, 0),
(47, 44, '配置删除', 2, NULL,         NULL,                    'system:config:del',  NULL,      3, 1, 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Audit log (under 系统管理): menu + view button.
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, status, visible, deleted) VALUES
(48, 2,  '审计日志', 1, 'audit',     'system/audit/index',  'system:audit:list', 'security', 11, 1, 1, 0),
(49, 48, '审计查看', 2, NULL,        NULL,                   'system:audit:list', NULL,       1, 1, 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Login log (under 系统管理): menu + view button.
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, status, visible, deleted) VALUES
(50, 2,  '登录日志', 1, 'login-log', 'system/login-log/index', 'system:loginlog:list', 'log', 12, 1, 1, 0),
(51, 50, '日志查看', 2, NULL,        NULL,                      'system:loginlog:list', NULL, 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Link permissions to roles.
-- platform-admin (id=1): everything above.
-- tenant-admin (id=2): notice publish only (matches prior role-based behaviour).
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(45, 1, 43),   -- platform-admin: notice publish
(46, 2, 43),   -- tenant-admin:  notice publish
(47, 1, 44),   -- platform-admin: config menu
(48, 1, 45),   -- platform-admin: config add
(49, 1, 46),   -- platform-admin: config edit
(50, 1, 47),   -- platform-admin: config del
(51, 1, 48),   -- platform-admin: audit menu
(52, 1, 49),   -- platform-admin: audit view
(53, 1, 50),   -- platform-admin: login log menu
(54, 1, 51)    -- platform-admin: login log view
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
