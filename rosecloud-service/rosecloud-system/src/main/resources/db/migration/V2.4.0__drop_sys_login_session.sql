-- Drops the legacy sys_login_session table. Session administration now lives in the
-- auth service (auth_login_session); the system service only uses the Redis session store.
DROP TABLE IF EXISTS sys_login_session;
