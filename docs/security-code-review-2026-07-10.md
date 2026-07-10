# 安全 Starter 代码审核报告

- 日期：2026-07-10
- 范围：`rosecloud-starter-security`、`rosecloud-common-security`、`rosecloud-service/rosecloud-auth`、`rosecloud-service/rosecloud-system`、`rosecloud-gateway`（安全相关）
- 方法：静态走读关键路径（签发/校验/刷新/吊销/登出/内部信任边界/租户写护栏），未做动态渗透。

> 说明：会话摘要中提到的 `JwtValidationException` / `SecurityContextFilter` / `AuthServiceImpl` / `TokenRevocationService` 等文件在实际仓库中并不存在。本次审核基于**磁盘上的真实代码**。

---

## 总体评价

代码质量整体较高，安全基线明显优于常见 starter：密钥强度强制、内部令牌常量时间比较、网关剥离 `X-Internal`、STATELESS + 安全响应头、刷新令牌轮换、租户状态拦截都做得到位。

**但存在 1 个架构级根因（吊销语义倒置）+ 若干中低风险点**，其中吊销模型在微服务 + 非共享存储场景下会导致认证整体不可用，需优先处理。

---

## 严重 / 高危 (HIGH)

### H1. 吊销语义倒置：「无会话即视为吊销」使「无状态 JWT」实为假态、且脆弱

- 位置：`SessionStore.isRevoked` Javadoc、`InMemorySessionStore.isRevoked`、`RedisSessionStore.isRevoked`、`JwtAuthenticationProvider#42`、`RefreshTokenAuthenticationProvider#44`
- 现状：`isRevoked(token) == true` 当且仅当**没有任何活跃会话包含该 token**。也就是说，一个 access token 要被接受，必须存在一条服务端 `LoginSession` 记录（由 `RestAwareAuthenticationSuccessHandler` 在登录/刷新时 `save`）。**有效性被「会话是否存在」门控，而非被「吊销集合」门控。**
- 影响：
  1. **无共享存储时跨服务认证整体失效**：只有 auth 服务 `save` 了会话；其它服务校验同一个 token 时本地无会话 → `isRevoked==true` → 全 401。`SessionStoreConfiguration` 在 Redis 不可用时会**静默回退到 `InMemorySessionStore`**（见 H2），微服务模式下必然踩坑。
  2. **重启即全员掉线**：InMemory 会话随进程丢失，所有在途 token 立即 401。
  3. **「禁用用户/改密立即生效」的承诺在 InMemory 下不成立**：`UserServiceImpl`/`RoleServiceImpl` 调用 `revokeByUserId` 只影响本地内存（且 system 服务本身不持有会话），因此多服务 + 非共享存储时，禁用/改密**不会**使在途 token 失效。`JwtAuthenticationProvider` 注释里的「立即生效」仅在共享 Redis 时为真。
- 建议：将 `isRevoked` 改为**真正的吊销集合**——按 `jti` 存储已吊销项，TTL = 该 token 剩余有效期；只要 token 通过签名+过期+issuer 且**不在**吊销集合内即接受。`save()` 降级为审计/管理用途（非有效性前提）。这样：
  - 资源服务器可纯签名校验（真正无状态）；
  - 吊销显式、可跨服务一致（经 Redis 共享）；
  - `revokeByUserId` 只需把该用户当前 `jti` 加入集合（或加「T 时刻前全部失效」标记）。

### H2. `InMemorySessionStore` 静默回退是微服务下的隐形地雷

- 位置：`SecurityConfiguration.SessionStoreConfiguration#120-136`
- 现状：`redisAwareSessionStore` 在 `StringRedisTemplate` bean 缺失时返回 `InMemorySessionStore`；由于它总会产出一个 `SessionStore` bean，`inMemorySessionStore` 兜底 bean 实际仅在 Redis 类不存在时才生效。效果是：**只要 Redis 没真正接上，就拿到一个会让跨服务认证全挂的 InMemory 存储**，且无任何告警。
- 建议：多服务部署下，若无共享存储（Redis）应**快速失败**（明确报错启动），或至少打出 ERROR 级告警。不要静默选 InMemory。

### H3. 登录/刷新无频率限制与账号锁定（暴力/撞库）

- 位置：`RestAuthenticationProvider`、`RefreshTokenProcessingFilter` 链路
- 现状：仅有 BCrypt(10) 拖慢，无失败计数、无锁定、无验证码、无网关级限速。
- 建议：基于 Redis 的失败次数计数 + 指数退避/锁定；或在网关层对 `/auth/login`、`/auth/refresh` 做限速。

### H4. 用户名枚举（差异化异常）

- 位置：`RestAuthenticationProvider#43-45`（抛 `UsernameNotFoundException` vs `BadCredentialsException`）、`JwtAuthSupport.loadAndValidateUser`（「User not found」/「User is disabled」/成功，信息可区分）
- 现状：攻击者可通过响应差异枚举有效用户名、甚至判断账号是否禁用。
- 建议：无论哪一步失败，统一抛 `BadCredentialsException("用户名或密码错误")`；避免 `UsernameNotFoundException` 向外传播到失败处理器。

---

## 中风险 (MEDIUM)

### M1. `RedisSessionStore` 忽略 `LoginSession.expireAt`，固定 7 天 TTL
- `save` 用固定 7 天 TTL，未取 `expireAt`（= refresh 有效期 24h）；`isRevoked` 也不参考 `expireAt`。后果：7 天内（即便 token 早已过期）token 仍解析到会话（非安全漏洞，因签名校验先于 isRevoked，但浪费存储且语义不一致）。
- 建议：键 TTL = `min(expireAt, 配置上限)`；`isRevoked` 将已过期的会话视为不存在。

### M2. 无 `aud` 受众声明，token 在所有服务通用
- 共享密钥下所有服务互认；多租户/零信任场景下建议加 `aud` 并按服务校验。

### M3. 令牌被盗即可完全重放，无设备/IP/UA 绑定
- `LoginSession` 采集了 `clientIp`/`userAgent`，但后续请求从不校验。被盗 bearer 可在任意来源使用。
- 建议：可选将 token/会话绑定指纹；不匹配时告警或挑战。至少文档化该假设。

### M4. `/error` 与 `/actuator/health/**` 为 permitAll
- `/error` 公开可能在某些流程下先于认证返回错误体。确认不泄露敏感信息；建议 actuator 仅暴露 health 并置于网络策略之后。

### M5. 租户写护栏依赖 `TenantLookupApi` 可用性
- `TenantWriteGuardFilter` 在无法解析租户状态（无 Feign user API）时**放行写操作**。这是「无 user API 的服务」的设计取舍，但意味着租户强制并非全局保证，取决于部署形态。建议文档化该依赖。

---

## 低风险 (LOW)

- **L1** `parseAccessToken` 把「存在 `type` 声明」一律视为非 access token（仅 refresh 显式带 `type=refresh`）。将来若加 `type=access` 会被拒。建议正向白名单或固化约定文档。
- **L2** `InMemorySessionStore.isRevoked` 每次请求 O(n) 且会 `evictExpired()` 改 map；有负载不要用，优先 Redis（代码注释已自承）。
- **L3** `SecurityProperties.internalToken` 为单一静态共享密钥（常量时间比较已做得好），但无轮换、无按服务凭证。网关剥离前提下可接受，注意泄漏面与日志脱敏。
- **L4** `LogoutProcessingFilter` 即使 token 解析失败也返回 200（记 debug、清上下文）——幂等登出，可接受，但畸形 token 也会得到「成功」响应。
- **L5** 登录响应体同时返回 access + refresh（SPA 常规）。确保 refresh token 不被日志打印、客户端安全存储。

---

## 已做得好的点（保持）

- 启动即拒绝空/过短 JWT 密钥，强制 HS512 64 字节（Base64 解码后）。
- 内部令牌 `MessageDigest.isEqual` 常量时间比较，修复了旧的长度泄露。
- 网关 `GatewayEdgeSecurityConfig` 在最高优先级剥离入站 `X-Internal`，内部信任边界成立（前提：仅网关对外、内网服务不直接暴露）。
- STATELESS、CSRF 恰当关闭、HSTS/frameOptions(sameOrigin)/nosniff/referrerPolicy/cacheControl 关闭 一应俱全。
- CORS 拒绝 `allowCredentials=true` 与通配 `*` origin 共存。
- 刷新令牌轮换 + 复用检测（`RefreshTokenAuthenticationProvider#55` revoke 旧 refresh，旧 access 也随之失效——这是正确的）。
- 租户状态在认证与写护栏两处拦截（DISABLED/PENDING 阻断）。
- access 与 refresh 两条路径都校验账号禁用。
- `JwtAuthSupport` 抽取了共享校验，避免重复。

---

## 优先级建议

1. **H1 + H2**：重构吊销模型为「吊销集合（按 jti）」，并让无共享存储的多服务部署快速失败。这是当前最大风险。
2. **H3 + H4**：加登录/刷新限速与统一认证失败响应（防暴力 + 防枚举）。
3. **M1/M2/M3**：Redis TTL 与 `expireAt` 对齐、引入 `aud`、可选令牌绑定。
4. **L1–L5**：按需清理。

> 注：本次为静态审核，未执行构建/测试。各模块 `target/classes` 存在，表明此前可编译；建议对 H1 重构后补 `isRevoked` 语义的单测（区分「未知但未吊销」与「已吊销」）。

---

## 修复状态（2026-07-10 续）

首轮已修复 H1/H2/H4/M1（吊销模型重构、InMemory 回退告警、用户名枚举、Redis TTL 对齐）。本轮按用户「全部一次性处理」补齐剩余项：**H3 / M2 / M3 / M4 / M5 / L1–L5**。编译与测试全绿（starter 25 个测试全 PASS，根编译全 SUCCESS）。

### H3 登录/刷新限速与账号锁定 ✅
- 新增 `BruteForceProtection`：基于 `StringRedisTemplate` 的失败计数，连续失败达 `maxFailedAttempts`（默认 5）即锁定 `lockoutDurationSeconds`（默认 900s）；成功登录/刷新清除计数。无 Redis 时降级为 no-op 并 WARN。
- `RestAuthenticationProvider`、`RefreshTokenAuthenticationProvider` 均接入：失败前 `assertNotLocked`、失败后 `onFailure`、成功后 `onSuccess`。
- 新增 `SecurityErrorCode.ACCOUNT_LOCKED`（HTTP 423）；`RestAwareAuthenticationFailureHandler` 对 `LockedException` 返回 423，其余失败仍统一 401（保持 H4 防枚举）。
- 配置：`rosecloud.security.brute-force.{enabled,max-failed-attempts,lockout-duration-seconds}`。测试：`BruteForceProtectionTest`。

### M2 增加 aud 受众声明并校验 ✅
- `JwtTokenFactory` 签发 access/refresh 写入 `aud`（默认 `rosecloud`，`rosecloud.security.jwt.audience`）；`parseTokenClaims` 内 `requireAudience` 校验，不匹配抛 `BadCredentialsException("Invalid JWT audience")`。
- 测试：`JwtTokenFactoryTest.tokenCarriesAudienceClaim` / `rejectsTokenWithMismatchedAudience`。
- 注意：部署后旧令牌（无 aud）会被拒绝，属预期（1h/24h 自然过渡）。

### M3 令牌 IP/UA 绑定（可选指纹）✅
- 新增 `DeviceFingerprint`（SHA-256(ip|ua)）。`SecurityProperties.TokenBinding.enabled`（默认 false）开启时，`RestAwareAuthenticationSuccessHandler` 签发时把指纹写入 token `fp` 声明。
- 校验 intrinsic 于 token：`JwtTokenAuthenticationProcessingFilter`（访问）、`RefreshTokenProcessingFilter`（刷新）认证成功后比对请求指纹，不匹配拒绝。仅对携带 `fp` 的令牌校验。

### M4 收敛 /error 与 /actuator/health 的 permitAll ✅
- `publicPaths` 默认值移除 `/error`（未认证错误转发返回正常 401，避免错误体提前泄露）。`/actuator/health/**` 保留 permitAll（探针用），注释强调须置于网络策略之后。

### M5 租户写护栏依赖文档化 + 可配置 fail-closed ✅
- `TenantWriteGuardFilter` 新增 `failClosed`（`rosecloud.security.tenant-write-guard.fail-closed`，默认 false）：为 true 且无法解析租户状态时阻断写（403），否则维持 fail-open。代码注释明确该依赖。

### L1–L5 ✅
- L1：`parseAccessToken` 改为仅拒绝 `type=="refresh"`（之前拒绝任何带 `type` 的令牌，会破坏未来 `type=access`）。
- L2/L3/L4/L5：分别在 `InMemorySessionStore`、 SecurityProperties、登出、成功处理器注释中说明/确认。

### 新增/修改文件
- 新增：`BruteForceProtection.java`、`DeviceFingerprint.java`、`JwtClaimsExtractor.readStringClaim`、`SecurityErrorCode.ACCOUNT_LOCKED`、测试 `BruteForceProtectionTest`。
- 修改：`SecurityProperties`、`JwtTokenFactory`、`RestAuthenticationProvider`、`RefreshTokenAuthenticationProvider`、`RestAwareAuthenticationSuccessHandler`、`JwtTokenAuthenticationProcessingFilter`、`RefreshTokenProcessingFilter`、`RestAwareAuthenticationFailureHandler`、`TenantWriteGuardFilter`、`SecurityConfiguration`。
