# Plan：rosecloud-auth 演进为 IAM 内核（项 2–7）

> 状态：基于 spec `docs/sdd/2026-07-13-auth-iam-core-spec.md`（v1.0）批复
> 推进顺序：A → B → C → D（用户确认）
> 切片原则：每片可独立编译、独立测试、可回滚；测试先行。
> 进度：**A 完成 / B 完成 / C 完成 / D 待做**。

## Slice A —— 会话持久化 + 审计增强（项3 + 项4，低风险、独立）

目标：会话落 `auth_login_session` 表（权威源）+ 保留 Redis 吊销；并发登录上限；设备信任；登录审计补齐 deviceId。

垂直切片 A1：会话 DB 表与实体
- 新增迁移 `V2.1__auth_login_session.sql`：建 `auth_login_session`（session_id 唯一、user_id、username、nickname、token、refresh_token、client_ip、user_agent、device_id、login_at、expire_at、revoked、标准审计列）。
- 新增 `LoginSessionEntity`（`@Getter @Setter @NoArgsConstructor`，MyBatis-Plus）、`LoginSessionMapper`。
- 验收：迁移可应用；实体可映射。关联 AC-4。

垂直切片 A2：DB-backed SessionStore（覆盖 RedisSessionStore）
- 在 auth 新增 `DbSessionStore implements SessionStore`：双写 DB + Redis（复用现有 Redis 键布局与 `revoked:` 机制）；`revoke`/`revokeByUserId`/`isRevoked`/`findAll` 同时维护 DB 与 Redis。
- auth 配置中以 `@Primary` 暴露 `DbSessionStore` bean，覆盖 starter 的 `RedisSessionStore`（边界要求 DB 持久化只在 auth 内）。
- 验收：AC-4、AC-5（并发上限见 A3）。

垂直切片 A3：并发登录上限 + 设备信任
- `LoginSession`(`common-security`) 增加 `deviceId` 字段；登录成功处理器用 `DeviceFingerprint` 派生并写入。
- `DbSessionStore.save`：保存前统计该 user 活跃会话数，超过配置上限（默认 5，`RoseCloudAuthProperties` 或复用 `RoseCloudDataProperties` 风格）则吊销最旧（按 `loginAt`/序号）。
- 验收：AC-5、AC-6（deviceId 落会话）。

垂直切片 A4：审计增强 deviceId
- `LoginLogRequest`(`rosecloud-api`) 增加 `deviceId`；`LoginLog` 域、`LoginLogEntity`、`auth_login_log` 表增加 `device_id` 列（迁移 `V2.2__auth_login_log_device.sql`）。
- 登录成功/失败事件构造 `LoginLogRequest` 时填充 `deviceId`（`AuthSecurityConfiguration` 事件处理器取 `DeviceFingerprint`）。
- 验收：AC-6。

垂直切片 A5：清理 stale 引用
- `rosecloud-common.yaml` 租户忽略表 `sys_login_session` → `auth_login_session`；`RoseCloudTenantLineHandlerTest` 同步。
- 验收：AC-10。

Slice A 验证：`./mvnw -o -pl rosecloud-service/rosecloud-auth -am test`（单元：DbSessionStore 并发/双写、LoginSessionController 列出来自 DB、LoginLog 记录 deviceId；集成如可用本地 MySQL+Flyway）。

## Slice B —— 凭证自持 + 密码策略收口（项5 + 项2，核心、高风险）

- 新增 `auth_credential` 表 + `CredentialEntity`/`CredentialMapper` + `CredentialStore`（starter 声明接口，auth 实现）。
- `PasswordPolicyValidator` 迁 `rosecloud-common-security`；auth 写密码时校验。
- 改 `AuthSecurityConfiguration` 的 `UserDetailsService` bean：组合 auth 凭证（密码哈希 + 认证态）+ system 档案/权限。
- system 侧：`UserServiceImpl`/`UserActivationService`/`UserController.me/password` 改为调用 auth 凭证 API 设密码；删除 system `user_credential` 密码存储。
- 新增 `auth` 凭证写 API（Feign `CredentialApi`），供 system 调用。
- 验收：AC-1、AC-2、AC-3。

## Slice C —— 授权集中（项6）【已完成】

- `UserDetailsService` 组合实现中经 `UserApi` 拉取权限，烘焙进 JWT claims（`JwtTokenFactory` 已支持 authorities）；session 缓存权限。
- 验收：AC-7。

### C 实现记录
- `JwtTokenFactory.createAccessJwtToken`：新增 `authorities` claim（`securityUser.getAuthorityStrings()`，空则不写），登录时把来自 system 的角色/权限烘焙进访问令牌。
- `JwtAuthenticationProvider.authenticate`：改为**直接从签名 claims 重建 `SecurityUser`**（含 `authorities`），移除每请求经 `userDetailsService.loadUserByUsername` 回查 system 的逻辑；tenant 状态校验（`TenantLookupApi`）仍保留（属租户治理，非用户鉴权回查）。新增 `JwtAuthenticationProviderTest.reconstructsAuthoritiesFromTokenClaims` 验证令牌中的权限在请求时被还原。
- `JwtAuthenticationProvider` 构造器移除 `UserDetailsService` 依赖；`SecurityConfiguration` 同步调整。`RefreshTokenAuthenticationProvider` 仍保留 `userDetailsService`（刷新为低频路径，刷新后新令牌自带 `authorities`）。
- 说明：权限一致性机制沿用既有 `sessionStore.revokeByUserId`（角色/状态变更即吊销会话，下次请求强制重新登录取得新令牌）。`LoginSession`/Redis 会话记录未冗余存储 authorities——令牌本身即为鉴权载体，符合"缓存于 session（令牌）"的意图。
- 关联遗留编译修复（分页/`ToData` 重构在 working tree 中未完全收口，顺带补齐以保证 reactor 可编译）：`PagedResults` 恢复带 mapper 的 `page` 重载；`TenantStatus` 补 `import java.time.LocalDate`；`AuditLogRepositoryImpl` 补 `import java.util.Optional`；`TenantProfileEntity` 补 `import com.fasterxml.jackson.databind.JsonNode`；`MenuServiceImpl`/`LoginLogServiceImpl`(auth) 的 `this::toDomain` 改为 `Entity::toData`；system 各 `CredentialApi` 调用点用 `CredentialSetRequest`/`CredentialChangeRequest` 包裹；`LoginSessionControllerTest` 改用 `PageQuery`/`PagedData`。
- 验证：`./mvnw -o test-compile`（全 reactor）BUILD SUCCESS；`rosecloud-starter-security` 测试 30 全过（含新增 authorities 还原用例）；auth `LoginSession*` 6 过、system `UserActivationServiceImplTest` 5 过。

## Slice D —— 自助服务（项7，无注册）

- auth 暴露 `forgot-password`/`reset-password`/`me/password`；一次性重置令牌表 `auth_password_reset`；经 notice Feign 发送。
- system `UserController.me/password` 改为调用 auth 凭证 API。
- 验收：AC-8、AC-9。

## 依赖顺序

A（独立）→ B（依赖 A 的会话/审计基础，且改登录流）→ C（依赖 B 的组合 UserDetailsService）→ D（依赖 B 的凭证 API）。
