-- Rebuilds sys_login_session with the redesigned schema.
-- The old table kept sessions per JWT jti with tenant context; the new
-- table tracks individual sessions per token, where one token may be
-- shared across multiple session rows (e.g. same token from different IPs).

DROP TABLE IF EXISTS sys_login_session;

CREATE TABLE IF NOT EXISTS sys_login_session (
    id   VARCHAR(64)   NOT NULL COMMENT 'Session identifier (UUID, primary key)',
    token        VARCHAR(768) NOT NULL COMMENT 'Access token (opaque string, can be shared across sessions)',
    user_id      BIGINT        NOT NULL COMMENT 'User ID',
    username     VARCHAR(64)   NOT NULL COMMENT 'Username at login time',
    nickname     VARCHAR(128)  DEFAULT NULL COMMENT 'Display name at login time',
    client_ip    VARCHAR(45)   DEFAULT NULL COMMENT 'Client IP address',
    user_agent   VARCHAR(512)  DEFAULT NULL COMMENT 'User-Agent header',
    login_at     DATETIME      NOT NULL COMMENT 'Login timestamp',
    expire_at    DATETIME      NOT NULL COMMENT 'Token expiry timestamp',
    PRIMARY KEY (id),
    INDEX idx_token (token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Login sessions for token-based authentication';
