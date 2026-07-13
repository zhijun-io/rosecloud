-- Login audit log migrated to rosecloud-auth (auth_login_log). Drop the legacy system-owned table.
DROP TABLE IF EXISTS sys_login_log;
