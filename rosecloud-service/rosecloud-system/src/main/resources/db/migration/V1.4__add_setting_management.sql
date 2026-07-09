-- Configuration management tables and backend permissions.
-- No frontend pages are added here; the buttons only grant API authorities.

CREATE TABLE IF NOT EXISTS sys_setting_key (
  setting_key VARCHAR(128) NOT NULL                   COMMENT '配置键',
  name        VARCHAR(128) NOT NULL                   COMMENT '配置名',
  remark      VARCHAR(255) DEFAULT NULL               COMMENT '备注',
  updated_at  DATETIME     DEFAULT NULL               COMMENT '更新时间',
  updated_by  BIGINT       DEFAULT NULL               COMMENT '更新人',
 PRIMARY KEY (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置键表';

CREATE TABLE IF NOT EXISTS sys_system_setting (
  setting_key VARCHAR(128) NOT NULL                   COMMENT '配置键',
  value       TEXT         DEFAULT NULL               COMMENT '配置值',
  updated_at  DATETIME     DEFAULT NULL               COMMENT '更新时间',
  updated_by  BIGINT       DEFAULT NULL               COMMENT '更新人',
  PRIMARY KEY (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

CREATE TABLE IF NOT EXISTS sys_user_setting (
  id          BIGINT       NOT NULL                   COMMENT '主键',
  user_id     BIGINT       NOT NULL                   COMMENT '用户ID',
  setting_key VARCHAR(128) NOT NULL                   COMMENT '配置键',
  value       TEXT         DEFAULT NULL               COMMENT '配置值',
  updated_at  DATETIME     DEFAULT NULL               COMMENT '更新时间',
  updated_by  BIGINT       DEFAULT NULL               COMMENT '更新人',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_setting_key (user_id, setting_key),
  KEY idx_user_id (user_id),
  KEY idx_setting_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户配置表';

-- API authorities for platform admins.
 -- NOTE: menu IDs 52-58 avoid collision with V1.3 (which used 43-51).
 INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, status, visible, deleted) VALUES
 (52, 2, '配置键查询', 2, NULL, NULL, 'system:setting-key:list', NULL, 10, 1, 1, 0),
 (53, 2, '配置键新增', 2, NULL, NULL, 'system:setting-key:add', NULL, 11, 1, 1, 0),
 (54, 2, '配置键修改', 2, NULL, NULL, 'system:setting-key:edit', NULL, 12, 1, 1, 0),
 (55, 2, '配置键删除', 2, NULL, NULL, 'system:setting-key:del', NULL, 13, 1, 1, 0),
 (56, 2, '系统配置查询', 2, NULL, NULL, 'system:setting:list', NULL, 14, 1, 1, 0),
 (57, 2, '系统配置修改', 2, NULL, NULL, 'system:setting:edit', NULL, 15, 1, 1, 0),
 (58, 2, '系统配置删除', 2, NULL, NULL, 'system:setting:del', NULL, 16, 1, 1, 0)
 ON DUPLICATE KEY UPDATE name = VALUES(name);
 
 INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
 (52, 1, 52), (53, 1, 53), (54, 1, 54), (55, 1, 55),
 (56, 1, 56), (57, 1, 57), (58, 1, 58)
 ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
