-- 补齐 sys_user.additional_info 列：实体 UserEntity 已映射该字段（结构化扩展信息 JSON），
-- 但历史迁移脚本未创建该列，导致登录时用户查询失败。此处补齐。
ALTER TABLE sys_user
    ADD COLUMN additional_info TEXT DEFAULT NULL COMMENT '扩展信息(JSON)' AFTER phone;
