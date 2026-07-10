# RoseCloud 主键与租户标识决策

## 1. 结论

RoseCloud 的业务主键仍采用应用侧生成的雪花风格 `Long` ID，不依赖数据库自增。

同时，租户主键统一为字符串：

- `tenantId`：租户主键 / 外键，类型为 `String`
- `sys_tenant.id` 就是租户身份本身，不再保留独立的租户编码字段
- `tenantId` 采用业务可读编码，统一规范为：字母开头、仅含字母和数字、长度不超过 10、入库前规范化为大写
- 系统租户保留值固定为 `ROOT`，普通租户不得使用该值

这意味着：

- 业务表中的 `tenantId` 继续表示“属于哪个租户”
- `tenantId` 直接承担租户身份，不再拆分成主键 + 编码两层语义
- 微服务模式下，`workerId` 和 `datacenterId` 不能靠人工逐个应用手填，应由部署层保证唯一性

## 2. 为什么这样定

### 2.1 主键需要统一

当前系统已经存在多种业务表和关联表，统一为 `Long` 雪花 ID 后：

- 跨服务写入不依赖数据库自增
- 更适合分布式写入和预创建关联
- 和 MyBatis-Plus 的 `IdType.ASSIGN_ID` 一致

### 2.2 `tenantId` 就是租户身份

`tenantId` 的职责是表达关系：

- 用户属于哪个租户
- 日志属于哪个租户
- 会话属于哪个租户
- 通知归属哪个租户

它本身就是业务展示和外键引用的统一标识。

## 3. 当前落地约束

### 3.1 数据库字段

- 所有业务主键统一使用 `BIGINT`
- 所有租户主键 / 外键统一使用 `tenant_id VARCHAR(...)`
- `sys_tenant.id` 使用 `VARCHAR(...)`
- `tenant_id` 的校验应由服务端统一完成，推荐正则为 `^[A-Z][A-Z0-9]{0,9}$`

### 3.2 MyBatis-Plus

- 主键实体默认使用 `IdType.ASSIGN_ID`
- 字符串主键实体可使用 `IdType.INPUT`，由服务层生成并赋值
- 业务表的租户外键继续使用字符串 `tenant_id`

### 3.3 微服务模式下的序列初始化

单体模式和微服务模式都使用 MyBatis-Plus 的雪花风格 `IdentifierGenerator`。

启动时自动读取当前应用所在机器的 IP 地址，并据此初始化 `DefaultIdentifierGenerator`。

这样可以避免给每个应用实例手工配置 `workerId` / `datacenterId`，也更适合 k8s 的自动扩容场景。

### 3.4 对外展示

当接口、日志、错误信息需要可读租户身份时：

- 直接返回 `tenantId`
- 不再返回单独的租户编码字段

### 3.5 历史数据清理

历史数据清理优先按时间字段而不是按 `id` 推断：

- 以 `create_time` / `update_time` 作为保留期和归档条件
- 大表清理可以先按时间筛选，再批量按 `id` 删除
- 不要把雪花 `id` 当成全局严格递增的时间游标

`id` 只适合做主键定位和关联，不适合作为跨应用历史数据清理的唯一依据。

## 4. 不做项

本决策明确不做：

- 把 `tenantId` 再拆回数值型
- 让业务表通过数值外键表达租户归属
- 让数据库自增作为主键策略
- 允许系统租户以外的记录占用保留值 `ROOT`

## 5. 现有代码对应

- `rosecloud-starter-data-mybatisplus` 的 `BaseEntity` 已使用 `IdType.ASSIGN_ID`
- `sys_tenant` 只保留 `id` 作为字符串主键
- `sys_user`、`sys_audit_log`、`sys_login_session` 等租户相关表继续使用 `tenant_id VARCHAR`

## 6. 落地建议

新表默认遵循：

1. 主键 `id BIGINT`
2. 租户主键 `id VARCHAR(...)`
3. 租户关联统一用 `tenant_id VARCHAR(...)`
4. 业务描述直接使用 `tenant_id` 或联表拿 `tenant_name`
