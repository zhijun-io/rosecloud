-- RoseCloud notice: announcement + read/confirm tables. Apply manually.
CREATE TABLE IF NOT EXISTS sys_notice (
  id               BIGINT       NOT NULL                   COMMENT '主键',
  title            VARCHAR(128) NOT NULL                   COMMENT '标题',
  content          TEXT                                    COMMENT '内容',
  target_type      TINYINT      NOT NULL                   COMMENT '投放:0全局 1租户 2角色',
  target_tenant_id VARCHAR(64)  DEFAULT NULL               COMMENT '租户目标(目标=租户时填)',
  target_role_code VARCHAR(64)  DEFAULT NULL               COMMENT '角色目标编码(目标=角色时填)',
  publish_type     TINYINT      NOT NULL DEFAULT 0         COMMENT '发布:0即时 1定时',
  publish_time     DATETIME     DEFAULT NULL               COMMENT '发布时间',
  effective_time   DATETIME     DEFAULT NULL               COMMENT '生效时间',
  expire_time      DATETIME     DEFAULT NULL               COMMENT '失效时间',
  status           TINYINT      NOT NULL DEFAULT 0         COMMENT '状态:0草稿 1已发布',
  need_confirm     TINYINT      NOT NULL DEFAULT 0         COMMENT '是否需确认:0否 1是',
  sender_id        BIGINT       DEFAULT NULL               COMMENT '发布人',
  tenant_id        VARCHAR(64)  DEFAULT NULL               COMMENT '发布人租户(平台为空)',
  channels        TINYINT      NOT NULL DEFAULT 1         COMMENT '通道位掩码:1站内 2邮件 4短信',
  create_time      DATETIME     DEFAULT NULL               COMMENT '创建时间',
  update_time      DATETIME     DEFAULT NULL               COMMENT '更新时间',
  create_by        BIGINT       DEFAULT NULL               COMMENT '创建人',
  update_by        BIGINT       DEFAULT NULL               COMMENT '更新人',
  deleted          TINYINT      NOT NULL DEFAULT 0         COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_status_publish (status, publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

CREATE TABLE IF NOT EXISTS sys_notice_record (
  id           BIGINT   NOT NULL              COMMENT '主键',
  notice_id    BIGINT   NOT NULL              COMMENT '通知ID',
  user_id      BIGINT   NOT NULL              COMMENT '用户ID',
  tenant_id    VARCHAR(64) DEFAULT NULL        COMMENT '租户ID',
  read_time    DATETIME DEFAULT NULL          COMMENT '阅读时间',
  confirm_time DATETIME DEFAULT NULL          COMMENT '确认时间',
  create_time  DATETIME DEFAULT NULL          COMMENT '创建时间',
  update_time  DATETIME DEFAULT NULL          COMMENT '更新时间',
  create_by    BIGINT   DEFAULT NULL          COMMENT '创建人',
  update_by    BIGINT   DEFAULT NULL          COMMENT '更新人',
  deleted      TINYINT  NOT NULL DEFAULT 0    COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_notice_user (notice_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知阅读确认记录表';
