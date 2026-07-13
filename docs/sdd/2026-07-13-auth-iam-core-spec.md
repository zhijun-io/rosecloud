# rosecloud-auth 演进为 IAM 内核（项 2–7）

> 状态：已批准（v1.0），推进顺序 A→B→C→D
> 触发：标准 IAM 对标，auth 当前仅为"JWT 签发 + 登录审计"网关
> 适用：rosecloud-auth、rosecloud-system、rosecloud-starter-security、rosecloud-common-security、rosecloud-api

## Goal

将 `rosecloud-auth` 从"JWT 签发 + 登录审计"升级为**自持凭证、集中授权、可治理**的认证服务：收口密码策略、持久化会话、增强审计、自持凭证、集中授权、提供受限自助服务（无注册）。

## Scope

- **项2** 密码策略与凭证校验收口到 auth（`PasswordPolicyValidator` 迁共享库，auth 写密码时校验；复用 `BruteForceProtection` 做失败锁定）。
- **项3** 在线会话持久化到 `auth_login_session` 表 + 每用户并发登录上限 + 设备信任 + 吊销；保留 Redis 吊销集。
- **项4** 登录审计增强（失败原因/设备/IP/UA 落库；MFA 事件暂缓）。
- **项5** auth 自持凭证（新增 `auth_credential`，system 移除密码存储；登录不再从 system 取密码）。
- **项6** 授权集中：auth 登录时拉取 system 权限并烘焙进 token/session；RBAC 主数据仍在 system。
- **项7** 自助服务（无注册）：找回密码、改密端点归 auth；MFA 登记暂缓。

## Non-goals

MFA 落地；OIDC/SAML/SCIM 联合；开放注册；RBAC 主数据迁 auth；业务用户/租户/角色管理迁 auth。

## Current Context

- 登录：`RestAuthenticationProvider`(`starter-security`) → `UserDetailsService` bean (`AuthSecurityConfiguration:54`) → 当前 Feign `UserApi.loadByUsername` 返回含密码哈希的 `SecurityUser` → `passwordEncoder.matches`。
- 会话：`SessionStore`(`common-security`) 是共享抽象；`RedisSessionStore`(`starter-security`) 为其 Redis 实现，登录成功由 `RestAwareAuthenticationSuccessHandler:70` 经 `SessionStore.save` 写入。auth 另有独立的 `LoginSessionService`(`LoginSessionServiceImpl`，Redis 实现) 仅被 `LoginSessionController.online` 用于列举会话，二者共享 Redis 键空间。
- 密码存储：system `user_credential`（`UserCredentialEntity.password/passwordChangedTime`）；`UserServiceImpl.insert/updatePassword`、`UserActivationService.confirm` 写哈希。
- 密码策略：`rosecloud-system/support/PasswordPolicyValidator`（长度/大小写/数字/特殊字符）。
- 审计：`LoginLogApi.record(LoginLogRequest{username,success,failReason,ip,userAgent})`，由 `LoginSucceededEvent`/`LoginFailedEvent` 触发（`AuthSecurityConfiguration:34-43`），落 `auth_login_log`。
- 令牌：`JwtTokenFactory` 签发；`SecurityUser` 携带 authorities（来自 system）。
- 边界：`docs/design/auth-system-boundary.md` §3 当前写"密码复杂度/有效期/账户过期 定义与存储 system"，与本次冲突，须修订。

## Requirements

- R1（项5）：auth 新增 `auth_credential`（密码哈希、密码修改时间、认证状态、失败计数、锁定截止、最后登录）。system 删除 `user_credential` 密码相关存储，登录不再经 Feign 取密码。
- R2（项2）：`PasswordPolicyValidator` 迁至 `rosecloud-common-security`（共享库，零微服务依赖），auth 在**写密码**时执行校验；system 创建/激活/改密改调 auth 凭证 API 设密码（auth 负责校验）。复用 `BruteForceProtection` 做账号锁定。
- R3（项3）：新增 `auth_login_session` 表；提供 DB -backed `SessionStore` 实现（在 auth 内，覆盖 starter 的 `RedisSessionStore`），双写 DB（权威源）+ Redis（吊销快查）；新增每用户最大并发会话数（配置项，默认 5），超限吊销最旧；`LoginSession` 增加 `deviceId`（由 `DeviceFingerprint` 派生）实现设备信任。
- R4（项4）：确保登录失败原因、`ip`、`userAgent`、`deviceId` 均落 `auth_login_log`；失败原因枚举化。
- R5（项6）：auth 登录时经 `UserApi` 获取用户档案 + 权限，组合成完整 `SecurityUser`（auth 凭证状态 + system 档案/权限）后签发 JWT，权限烘焙进 token claims 并缓存于 session；`UserDetailsService` bean 改为"auth 凭证 + system 档案/权限"的组合实现。
- R6（项7）：auth 暴露 `POST /api/auth/forgot-password`、`POST /api/auth/reset-password`、`POST /api/auth/me/password`（改密）；找回密码使用一次性随机令牌（≥32 bytes、短期、单次），经 notice 服务发送；system 现有 `UserController.me/password` 改为调用 auth 凭证 API。

## Acceptance Criteria

- AC-1（项5）：当 auth 拥有 `auth_credential` 中某用户的密码哈希时，登录校验只读取 auth 本地凭证，不再经 Feign 从 system 获取密码哈希。
- AC-2（项5）：当 system 删除 `user_credential` 后，现有用户创建/激活/改密流程仍能正确设置密码（调用 auth 凭证 API），且业务用户与凭证最终一致。
- AC-3（项2）：当 auth 写密码时策略不合规，凭证写入被拒并返回明确错误；`BruteForceProtection` 在连续失败达阈值后锁定账号，登录前置 `assertNotLocked` 拒绝。
- AC-4（项3）：当一次登录成功，`auth_login_session` 表与 Redis 均出现该会话记录；`LoginSessionController` 列出的会话与 DB 一致。
- AC-5（项3）：当某用户活跃会话数超过配置上限，最旧的会话被吊销（Redis revoked 集 + DB 标记失效）。
- AC-6（项4）：当登录失败，`auth_login_log` 记录含失败原因、ip、userAgent、deviceId。
- AC-7（项6）：当 auth 签发 JWT，token claims 含该用户来自 system 的角色/权限，且后续请求不再需要回查 system 即可鉴权。
- AC-8（项7）：当调用 `forgot-password`，无论账号是否存在都返回统一响应，且不泄露账号是否存在；`reset-password` 使用一次性令牌，重用或过期均失败。
- AC-9（项7）：当调用 `me/password` 改密，当前密码错误时被拒；成功后该用户既有会话按现有机制失效。
- AC-10（边界）：`docs/design/auth-system-boundary.md` §3 修订为"凭证与密码策略执行归 auth，RBAC 主数据/业务档案归 system"，与其余规则无矛盾；`rosecloud-common.yaml` 与租户测试里 stale 的 `sys_login_session` 改为 `auth_login_session`。

## Constraints

- 维持 auth/system 互不编译依赖；auth 经 Feign **只读** system 档案/权限，密码写入仅发生在 auth。
- 密码哈希离开 system 是跨服务改动：`UserActivationService`/`UserService.create`/`UserController.me/password` 须改为调用 auth 凭证 API；需保证幂等 + 最终一致（凭证与业务用户）。
- `starter-security` 保持 DB 无关：会话 DB 持久化只能发生在 auth 内（由 auth 提供 `SessionStore` 实现）。
- JWT 结构与现有网关/前端兼容：authorities 仍由 system 提供、auth 烘焙，不改变 claims 形态。
- 重置令牌加密随机 ≥32 bytes、TTL 短、单次使用；防账号枚举（统一错误）。

## Decisions

- 凭证表放 auth；`PasswordPolicyValidator` 迁 `rosecloud-common-security`（共享、零微服务依赖），auth/system 共用。
- 会话：DB 为权威源，Redis 仅作吊销快查（保留现有 `revoked:` 机制）；DB 持久化由 auth 内的 `SessionStore` 实现承担，覆盖 starter 的 `RedisSessionStore`。
- 找回密码通知复用现有 notice Feign（与"OTP 经 notice"一致）。
- 设备信任：`LoginSession` 增加 `deviceId`，由服务端 `DeviceFingerprint` 派生，无需客户端改动。

## Related ADRs

- `docs/design/auth-system-boundary.md`（本次须修订 §3）

## Revision log

- 2026-07-13 | 初版批准，确认推进顺序 A→B→C→D | none | 进入 sdd-plan
