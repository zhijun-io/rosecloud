# RoseCloud 开发计划

目标：先把后端闭环跑通，不做前端页面。优先完成“租户直开 -> 首个管理员激活 -> 初始化 -> 通知 -> 审计”这条主链路。

## 阶段1. 已实现功能

目标：沉淀当前已经落地的底座能力，作为后续开发的已知基线。

### 1.1 平台底座

- Maven 多模块工程已搭好，按 `common` / `api` / `starter-*` / `service` / `monolith` 分层
- 本地运行编排已具备，包含 `Taskfile.yml` 和 `docker-compose.yml`
- 单体与微服务两种入口都已预留

### 1.2 认证与安全

- JWT 认证链路已实现，包含 access / refresh 签发与校验
- 令牌吊销已实现，支持按 `jti` 撤销
- `SecurityContext` 与 `UserContext` 已有贯通
- Feign 出站身份透传已实现
- Feign 调用链路的共享密钥守卫已实现
- OAuth2 资源服务器、方法级授权、公开路径白名单已预留并接入基础装配

### 1.3 多租户基础

- 租户上下文已实现，多租户默认启用（列级 COLUMN 隔离为默认主路径；`rosecloud.tenant.type=NONE` 时为 no-op）
- 平台管理员归属**系统租户**（保留租户，id 为全零 UUID `00000000-0000-0000-0000-000000000000`）；系统租户上下文为平台视角，行级隔离豁免，可见全部租户数据
- 活动租户由服务端签发的 JWT `tenant` claim 承载；客户端 `X-Tenant-Id` 头在网关注入层被剥离，下游只信任已认证主体
- Servlet Filter 从已认证主体派生租户（运行于 Spring Security 之后），`@Async` 线程池租户透传已实现
- MyBatis-Plus 的租户行级隔离已接入，系统租户豁免
- 受控租户切换已实现：`GET /api/auth/tenants` 列出可切租户，`POST /api/auth/switch-tenant` 校验成员关系后重发令牌并吊销旧会话

### 1.4 审计与日志

- `@AuditLog` 切面已实现
- 审计事件发布与日志监听已实现
- 系统内已有登录日志、操作审计、审计日志的基础落点

### 1.5 系统管理

- 用户管理、角色管理、菜单管理、部门管理、字典管理、登录会话管理、登录日志、审计日志等基础能力已落地
- 租户管理已具备创建、开通、启停和列表接口
- 角色菜单授权已具备基础接口

### 1.6 通知中心

- 通知发布、列表、我的通知、已读、确认等主流程已实现
- 定时发布和异步派发已接上
- 站内通知、邮件、短信的基础分发骨架已具备

### 1.7 共享基础设施

- 缓存 starter 已实现本地缓存与 Redis 缓存两种后端
- Trace starter 已实现请求链路标识生成与透传
- Web starter 已提供全局异常处理
- MyBatis-Plus starter 已提供分页与审计填充基础装配

## 阶段2. 当前计划

目标：把租户创建、开通、激活、初始化、通知和审计串成一条可验收的后端闭环。

### 2.1 租户创建与开通

#### 模块边界

- `system` 负责租户创建、开通、状态流转和初始化编排。
- `notice` 负责开通结果、激活结果和后续触达通知。
- `auth` 负责首次登录、密码设置、会话和上下文。
- `audit` 负责关键动作的审计记录与追踪。

#### 状态机

- 系统管理员创建租户后进入 `PENDING`
- 开通完成后进入 `ENABLED`
- 管理员停用后进入 `DISABLED`
- 到期后进入 `EXPIRED`

#### 接口清单

- `POST /api/system/tenants`
- `GET /api/system/tenants`
- `POST /api/system/tenants/{id}/open`
- `POST /api/system/tenants/{id}/disable`
- `POST /api/system/tenants/{id}/enable`
- `GET /api/system/tenants/{id}` 或等价详情接口
- `GET /api/system/tenants/{id}/audit` 或等价审计查询接口

#### 验收

- 能创建租户
- 能查询租户
- 能开通并看到状态变化
- 能明确区分待开通、已开通、已停用、已过期状态

### 2.2 租户开通与激活

#### 模块边界

- `system` 编排开通和初始化动作。
- `auth` 提供首个管理员激活、设密和首次登录所需的认证基础，且只负责凭据与会话，不承载用户主体字段。
- `notice` 发送开通结果和激活通知。

#### 状态机

- `PENDING` 的租户可以进入开通
- 开通完成后变为 `ENABLED`
- 首个管理员通过激活链接或邮件完成激活并设置密码
- 激活 token 有时效，过期后可重新发送
- 重新发送时生成新的激活 token，旧 token 立即失效
- 开通失败保持 `PENDING` 或进入可重试失败态，具体失败态可后续补充

#### 接口清单

- `POST /api/system/tenants/{id}/open`
- `POST /api/auth/activation/confirm`
- `POST /api/auth/change-password`
- `GET /api/system/roles`
- `GET /api/system/menus`

#### 验收

- 租户从 `PENDING` 能走到 `ENABLED`
- 首个管理员能被初始化出来
- 默认角色、默认菜单、基础配置都已创建
- 凭据数据与用户主体数据分离，密码不落在 `sys_user`
- 首个管理员能通过激活链接或邮件完成首次登录并设置密码

### 2.3 通知闭环

#### 模块边界

- `notice` 负责通知本身、通知派发和通知消费。
- `system` 在租户开通、激活后发出业务事件或调用通知入口。

#### 状态机

- 通知发布后进入已发布或待发布状态
- 定时通知在到点后切换到已发布
- 已发布通知可被目标用户读取、已读、确认

#### 接口清单

- `POST /api/notice/notices`
- `GET /api/notice/notices`
- `GET /api/notice/notices/me`
- `GET /api/notice/notices/me/{id}`
- `POST /api/notice/notices/me/{id}/read`
- `POST /api/notice/notices/me/{id}/confirm`

#### 验收

- 开通结果和激活结果可通知
- 激活结果可通知
- 相关人员能在站内看到结果
- 通知记录可按用户查看与消费

### 2.4 登录侧租户语义

#### 模块边界

- `auth` 负责登录、登出、令牌签发、租户上下文与会话登记；JWT 保存 `username`、用户 id、`enabled`、有效活动租户 `tenant` 等最小字段，角色/权限等业务上下文登录后由服务端按 `username` 补全。
- `auth` 负责会话查询、强制下线、在线会话管理、租户切换。
- `notice` 不参与认证语义，只消费用户上下文。

#### 状态机

- 平台管理员归属系统租户（id 全零 UUID），登录时活动租户为系统租户，无需显式租户标识
- 普通用户登录时活动租户取其归属租户（来自 `sys_user.tenant_id` / `sys_user_tenant` 成员关系）
- 当前仅提供用户名密码登录，不在同一接口中混合验证码等其他登录方式
- JWT 携带有效活动租户 `tenant` claim；租户、角色、权限等为业务上下文，登录/刷新时由服务端按 `username` 还原，变更后通过吊销会话强制重发令牌生效
- `user_credential.password_changed_time` 之后签发的旧 access / refresh token 视为失效
- 登录成功后生成有效会话
- 登出后按 `jti` 吊销令牌并下线会话

#### 接口清单

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/tenants`（列出当前用户可切入的租户）
- `POST /api/auth/switch-tenant`（切换当前活动租户，重发令牌）
- `GET /api/auth/sessions/online`
- `DELETE /api/auth/sessions?sessionId={id}`

#### 验收

- 不同角色按不同租户语义登录（平台管理员=系统租户视角，普通用户=归属租户）
- 登录后上下文正确，活动租户写入 JWT `tenant` claim
- 登出后旧令牌失效
- 密码/角色/权限变更后旧 access / refresh token 失效（会话被吊销）
- 在线会话可查询且可强制下线
- 登录凭据仅存在于 `user_credential`，`sys_user` 不存密码
- 租户切换仅可在授权范围内进行，越权切换被拒绝且不签发新令牌

### 2.5 审计与可追踪性

#### 模块边界

- `audit` 记录创建、开通、激活、初始化、登录等关键动作。
- `system` 和 `auth` 在关键入口处触发审计。

#### 状态机

- 审计事件在动作发生时产生
- 审计日志可按租户、操作人、动作查询

#### 接口清单

- `GET /api/system/audit/logs`
- `GET /api/system/audit/logs/{id}` 或等价详情接口

#### 验收

- 平台管理员能追溯整条租户链路
- 关键动作有审计记录
- 审计信息包含操作者、租户、动作、时间

### 2.6 配置模型决策

配置管理采用最小可用模型：

- `setting_key` 负责系统设置和用户设置的 key 定义
- `system_setting` 负责平台级设置，字段为 `key / value / updated_time / updated_by`
- `user_setting` 负责用户级设置，字段为 `user_id / key / value / updated_time / updated_by`
- `tenant_profile` 负责租户套餐、配额和能力画像，不纳入 key/value 体系
- `customer` 暂不独立设置
- 实体扩展信息继续放在实体自身的 `extra` 字段中

时间类字段统一使用 `_time` 后缀，新建表和新字段不再采用 `_at` / `_ts`。

设计原则：

- 表就是作用域，`system_setting` 和 `user_setting` 不需要 `scope`
- `key` 负责命名空间，避免重复和语义漂移
- `value` 统一承载值，简单值字符串化，复杂值用 JSON 字符串
- `extra` 只做扩展，不承载规则、继承或权限
- 不做统一覆盖链，不做通用 `SettingResolver`

落地现状：

- `system` 模块已实现 `setting_key`、`system_setting`、`user_setting` 的后端接口
- 系统配置和用户配置都按 `key / value` 读写
- 配置键删除时会同步清理对应的系统配置和用户配置

### 2.7 不做项

- 不做前端页面
- 不做公共入口区
- 不做工作台页面
- 不扩展新的基础设施 starter

### 2.8 推荐执行顺序

1. `system` 创建/开通/激活
2. `system` 初始化链路
3. `notice` 通知结果
4. `auth` 租户感知登录
5. `audit` 审计补强

### 2.9 测试要求

- 租户创建与开通：
  - 覆盖字段校验、状态流转、重复编码、缺失字段、非法状态
- 租户开通与激活：
  - 覆盖创建首个管理员、默认角色、默认菜单、基础配置
  - 覆盖开通幂等、失败回滚或失败可重试语义
  - 覆盖激活链接 / 邮件、首次登录、密码设置、临时 token 过期重发
- 通知闭环：
  - 覆盖发布、定时发布、已读、确认
  - 覆盖站内通知可见性与目标范围
- 登录侧租户语义：
  - 覆盖平台管理员、租户管理员、普通用户的登录差异
  - 覆盖登出、令牌吊销、会话下线
  - 覆盖当前仅用户名密码登录，其他登录方式走独立接口的约束
- 审计与追踪：
  - 覆盖关键动作有审计记录
  - 覆盖审计中能查到操作者与租户上下文
