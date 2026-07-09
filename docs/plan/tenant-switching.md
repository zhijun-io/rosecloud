# 多租户成员关系与受控租户切换 实现方案

状态：待实施（Draft）
基线提交：`1c0aead`
关联决策：`docs/adr/id-and-tenant-model-decision.md`

## 1. 背景与目标

当前实现将租户绑定为「一个用户一个固定租户」：租户唯一来源是认证主体
`SecurityUser.getTenantId()`，由 `UserRepositoryImpl.loadByUsername` 从 `sys_user.tenant_id`
单值列加载；`TenantWebFilter` 只从已认证主体取租户并**忽略** `X-Tenant-Id`，网关
`TenantGatewayFilter` 还会主动剥离该头。这套设计修复了「伪造头越权跨租户」的问题，但
也意味着用户无法归属多个租户，更无法切换。

本方案目标：

- 支持「一个用户属于多个租户」的成员关系模型。
- 支持用户在其**授权范围内**受控切换当前活动租户（active tenant）。
- 支持平台管理员（无归属租户）以受控方式「代入」某个租户操作。
- 全程不信任客户端 `X-Tenant-Id`，活动租户始终绑定到已认证身份并由服务端签发的令牌承载。

非目标（不做项）：

- 不引入前端页面（沿用 `docs/plan/development-plan.md` 的后端优先原则）。
- 不改变 `tenantId` 为字符串主键的既定决策（见 ADR）。
- 不做「一个请求同时跨多租户读写」，切换是「切当前上下文」，不是「聚合多租户」。

## 2. 现状与约束（关键代码位置）

- 认证主体：`rosecloud-common/rosecloud-common-security/.../model/SecurityUser.java`
  —— 已含单值 `tenantId` 字段（不可变对象，含 `fromJson` 工厂）。
- 用户加载：`rosecloud-service/rosecloud-system/.../persistence/UserRepositoryImpl.java:51`
  `loadByUsername`，第 71 行构造 `SecurityUser`，`tenantId` 取 `po.getTenantId()`（归属租户）。
- 令牌工厂：`rosecloud-starter/rosecloud-starter-security/.../token/JwtTokenFactory.java`
  —— 现有 claim：`userId` / `nickname` / `enabled` / `type`（refresh 标记）。access token
  当前**未**携带租户 claim。
- 访问令牌校验：`.../security/auth/jwt/JwtAuthenticationProvider.java`
  —— 校验签名/吊销/`isEnabled` 后，用 `loadUserByUsername` 重新加载主体。
- 刷新令牌校验：`.../security/auth/jwt/RefreshTokenAuthenticationProvider.java`。
- 租户上下文注入：`rosecloud-starter-business/rosecloud-starter-tenant/.../web/TenantWebFilter.java`
  —— `resolveTenantId()` 从 `SecurityContextHolder` 主体取 `getTenantId()`。
- 认证入口：`.../security/config/SecurityConfiguration.java`
  `LOGIN_ENTRY_POINT=/api/auth/login`、`REFRESH_ENTRY_POINT=/api/auth/refresh`、
  `LOGOUT_ENTRY_POINT=/api/auth/logout`。
- 数据库迁移目录：`rosecloud-service/rosecloud-system/src/main/resources/db/migration/`
  （Flyway，当前最高版本 `V2.2.1`）。
- 用户表：`sys_user`（含 `tenant_id VARCHAR`，见 `UserEntity`）。

关键约束：**令牌由本服务 HS512 签名，不可篡改**。因此活动租户一旦在「切换」时完成成员
校验并写入 access token 的签名 claim，后续每个请求可直接信任该 claim，无需每请求回查成员关系。

## 3. 设计方案

### 3.1 数据模型：成员关系表 `sys_user_tenant`

用户与租户多对多。归属租户（`sys_user.tenant_id`）视为「主租户」，同时也应在成员表中冗余一
行，便于统一查询（迁移脚本负责回填）。

字段建议：

- `id BIGINT` 主键（`IdType.ASSIGN_ID`）
- `user_id BIGINT` 用户
- `tenant_id VARCHAR(64)` 租户
- `is_primary TINYINT` 是否主租户（0/1）
- `create_time` / `update_time`
- 唯一索引 `(user_id, tenant_id)`；辅助索引 `(user_id)`、`(tenant_id)`

平台管理员：`sys_user.tenant_id` 为 `NULL`，成员表可不落行；其「可切入任意租户」的能力由
角色判定（见 3.5），不依赖成员表。

### 3.2 令牌模型：新增 `tenant` claim（活动租户）

- 在 `JwtTokenFactory` 增加常量 `TENANT = "tenant"`。
- `createAccessJwtToken(SecurityUser)`：写入 `.add(TENANT, securityUser.getTenantId())`
  （登录时活动租户 = 主租户）。
- 新增重载 `createAccessJwtToken(SecurityUser, String activeTenantId)` 与
  `createTokenPair(SecurityUser, String activeTenantId)`，供「切换」时以指定活动租户签发。
- refresh token 也写入 `tenant` claim，保证刷新后活动租户不丢失。
- `parseAccessToken` 无需改动；读取 claim 在 Provider 中进行。

### 3.3 每请求：活动租户注入为「有效主体」

`JwtAuthenticationProvider.authenticate` 调整：

1. 现有流程不变：校验签名 → 吊销 → `loadUserByUsername` → `isEnabled`。
2. 读取 access token 的 `tenant` claim：
   - claim 存在 → 作为有效活动租户；
   - claim 缺失（老令牌）→ 回退用主体的主租户（向后兼容）。
3. 用有效活动租户构造返回主体：`securityUser.withTenantId(activeTenant)`。

因为 claim 来自我方签名令牌，**不需要**每请求回查成员关系（成员被移除时靠令牌吊销/过期收敛）。

为此在 `SecurityUser` 增加不可变拷贝方法：

```java
public SecurityUser withTenantId(String newTenantId) {
    return new SecurityUser(userId, username, nickname, password, enabled,
            newTenantId, userPrincipal, getAuthorities());
}
```

`TenantWebFilter` 无需改动：它仍读 `SecurityUser.getTenantId()`，此时已是被验证过的活动租户。

### 3.4 切换与查询接口（auth 服务）

新增 `TenantSwitchController`（`rosecloud-service/rosecloud-auth/.../controller/`）：

- `GET /api/auth/tenants`
  返回当前用户可切入的租户列表（成员集合，平台管理员另行说明）。
- `POST /api/auth/switch-tenant`  body：`{ "tenantId": "..." }`
  流程：
  1. 从 `SecurityContext` 取当前用户 `userId`；
  2. **服务端校验** 目标租户 ∈ 用户成员集合（或用户是平台管理员，见 3.5）；不通过 → `403`；
  3. 用 `createTokenPair(securityUser, targetTenantId)` 重新签发 access/refresh；
  4. 旧令牌吊销（复用 `SessionStore.revoke` / 现有登出逻辑），返回新令牌对。

成员校验数据在 system 服务，auth 通过 Feign 获取。新增契约（`rosecloud-api`）：

- `UserTenantApi`（system 提供）：
  - `List<String> listTenantIds(Long userId)`
  - `boolean isMember(Long userId, String tenantId)`
- system 侧实现：`sys_user_tenant` 查询 + 主租户并入结果。

> 说明：切换只需在「切换那一刻」查一次成员关系；日常每请求走 3.3 的签名 claim，零额外查询。

### 3.5 平台管理员「代入租户」

- 判定：主体具备平台管理员角色（如 `ROLE_platform-admin`，以现有角色码为准，代码中以常量收口）。
- `switch-tenant` 对平台管理员放行任意存在的租户（可选：校验租户存在且状态正常）。
- 代入后签发的令牌 `tenant` claim = 目标租户，其请求即被行级隔离限定到该租户；
  代入前（无 claim 或 claim 为空）保持「无租户上下文 → 平台视角」。
- 审计：`switch-tenant` 应写审计日志（复用 `@AuditLog`），记录 who / 代入哪个租户。

### 3.6 兼容性

- 老 access token 无 `tenant` claim → 回退主租户，行为与当前一致。
- 单租户用户：成员集合仅含主租户，切换无实际可选项（`GET /api/auth/tenants` 返回单元素）。
- 迁移脚本回填：为所有 `sys_user.tenant_id` 非空用户插入一行 `is_primary=1` 的成员记录。

## 4. 分步实施（建议顺序，每步可独立验证）

1. **DB 迁移**：新增 `db/migration/V2.3.0__add_user_tenant_membership.sql`
   建 `sys_user_tenant` 表 + 唯一/辅助索引 + 回填主租户成员。
   验证：`./mvnw -pl rosecloud-service/rosecloud-system -am -Dmaven.test.skip=true install` 通过；
   本地 Flyway 迁移成功、回填行数 = 非空 `tenant_id` 用户数。

2. **持久化层**：`UserTenantEntity` + `UserTenantMapper` + `UserTenantRepository(Impl)`
   （`rosecloud-service/rosecloud-system/.../persistence/`），提供 `listTenantIds` / `isMember`。
   验证：`UserTenantRepositoryImplTest`（参照现有 `UserRepositoryImplTest` 的 Testcontainers/MyBatis 风格）。

3. **契约**：`rosecloud-api` 增加 `UserTenantApi` + 相关 record；system 侧新增
   `UserTenantController` 实现内部端点（走现有内部鉴权，参考 `InternalApi`/内部过滤器）。
   验证：编译 + `TenantSwitchController` 可通过 Feign 注入。

4. **SecurityUser 拷贝方法**：新增 `withTenantId(String)`。
   验证：`SecurityUserJsonTest` 补充一条 `withTenantId` 断言（拷贝后其余字段不变）。

5. **令牌**：`JwtTokenFactory` 增加 `tenant` claim 与带活动租户的重载。
   验证：新增 `JwtTokenFactoryTest`：签发含 `tenant` claim → 解析回读一致；带活动租户重载生效。

6. **访问/刷新校验**：`JwtAuthenticationProvider` / `RefreshTokenAuthenticationProvider`
   读取 `tenant` claim 并用 `withTenantId` 构造有效主体；claim 缺失回退主租户。
   验证：单测覆盖「有 claim 用 claim」「无 claim 回退主租户」两分支。

7. **切换接口**：`TenantSwitchController`（`/api/auth/tenants`、`/api/auth/switch-tenant`），
   含成员校验、平台管理员放行、旧令牌吊销、审计。
   验证：切换到成员内租户 → `200` 且新令牌 `tenant` claim 更新；切换到非成员 → `403`。

8. **文档**：更新 `docs/plan/development-plan.md`（多租户小节）与 `README`（如涉及）；
   如切换语义构成架构决策，补一条 ADR。

## 5. 验收标准（可机器校验）

- 成员用户 `POST /api/auth/switch-tenant` 到其成员租户：返回新令牌，后续请求数据被限定到目标租户。
- 用户切换到**非成员**租户：返回 `403`，且不签发新令牌。
- 携带 `tenant=B` 的令牌请求，即使再传 `X-Tenant-Id: C`，实际生效仍为 B（头被忽略/网关剥离）。
- 老令牌（无 `tenant` claim）请求：行为等同主租户，不报错。
- 平台管理员可代入任意存在租户；非管理员不能。
- 受影响模块单测全绿：
  `./mvnw -o test -pl rosecloud-common/rosecloud-common-security,rosecloud-starter/rosecloud-starter-security,rosecloud-service/rosecloud-system,rosecloud-service/rosecloud-auth`。

## 6. 安全边界

- 任何时候都不以客户端 `X-Tenant-Id` 作为租户来源（`TenantWebFilter` 已只信任主体，网关已剥离头）。
- 活动租户只能通过 `switch-tenant`（经成员校验）写入签名令牌来变更。
- 切换必须吊销旧令牌，避免「旧租户令牌」与「新租户令牌」并存扩大暴露面。
- 平台管理员代入能力必须基于角色判定并写审计。

## 7. 风险与回滚

- 风险：令牌体积增加一个 claim（可忽略）；成员被移除后旧令牌在过期/吊销前仍有效
  —— 通过缩短 access token TTL + 依赖吊销收敛。
- 风险：迁移回填遗漏历史脏数据（`tenant_id` 指向不存在租户）—— 迁移前先做一致性校验。
- 回滚：本方案为增量。回滚只需停用 `switch-tenant` 与 `tenant` claim 读取（Provider 回退主租户
  分支即为兼容路径），`sys_user_tenant` 表可保留不影响既有单租户行为。
