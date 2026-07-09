# RoseCloud 代码审核报告

> 审查范围：安全/认证、system 业务、starter 技术模块、common/notice/monolith 等全量源码（约 328 个 Java 文件，不含 `target`）。
> 结论：整体分层清晰、契约驱动、无明显 SQL 注入与明文密码问题；但存在若干**高危安全缺陷**与若干**影响正确性/可用性的中危问题**，建议优先处理。

---

## 一、高危（High）

### H1. `X-Internal` 头被无条件信任，无网关剥离 → 内部接口越权
- 文件：`rosecloud-starter/rosecloud-starter-security/.../web/InternalApiAuthenticationFilter.java:35-61`
- 现象：只要请求携带 `X-Internal` 头即被授予 `ROLE_INTERNAL`，注释明确写着「No secret is verified here」，依赖「网关会剥离外部请求中的该头」。
- 问题：审查 `rosecloud-gateway` 模块（仅有 `MetadataRouteDefinitionLocator`、`RouteRefreshScheduler`，无 `GlobalFilter`/`WebFilter` 剥离该头），**网关并未做任何剥离**。任何外部客户端发送 `X-Internal: true` 即可访问所有 `@InternalApi` 端点。
- 修复：在网关边缘用响应式 `GlobalFilter` 路由前移除 `X-Internal`；或将信任升级为带签名的内部令牌（注释中提到的 Tier B，HMAC 校验）。

### H2. 在线会话管理接口恒为 403，访问控制逻辑矛盾
- 文件：`rosecloud-service/rosecloud-auth/.../controller/LoginSessionController.java:27-44`、
  `rosecloud-starter/.../config/SecurityConfiguration.java:82-87, 222-230`
- 现象：`/api/auth/sessions/**` 受 `@PreAuthorize("hasAuthority('system:session:list'|'system:session:kick')")` 保护；但 `SecurityConfiguration` 将 `/api/auth/**` 放入 `publicPaths`（permissive），且 JWT 过滤器对 `publicPaths` 及 `X-Internal` 头请求跳过鉴权。结果：JWT 从不建立主体，`InternalApiAuthenticationFilter` 只给 `ROLE_INTERNAL`，永远不满足 `system:session:*`，**所有请求恒返回 403**。
- 修复：将 `/api/auth/sessions`（或 `/api/auth/sessions/**`）从 `publicPaths` 中移除，使 JWT 过滤器为调用方建立主体，再由方法级 `@PreAuthorize` 评估权限；或仅在下游调用方 `SecurityUser` 实际携带 `system:session:*` 时才用 `@InternalApi`。

### H3. 缓存 Redis 后端永远不生效（`@ConditionalOnMissingBean` 顺序 bug）
- 文件：`rosecloud-starter/rosecloud-starter-cache/.../CacheAutoConfiguration.java:30-46`
- 现象：`localRoseCloudCache` 仅用 `@ConditionalOnMissingBean(RoseCloudCache.class)` 且无 `type` 条件，先于嵌套的 `RedisCacheConfiguration` 注册；故即使配置 `rosecloud.cache.type=redis` 且存在 Redis，嵌套 Redis Bean 的 `ConditionalOnMissingBean` 已为 false，**始终得到本地缓存**。跨实例共享缓存特性实际失效。
- 修复：给本地 Bean 增加 `@ConditionalOnProperty(prefix="rosecloud.cache", name="type", havingValue="in-memory", matchIfMissing=true)`（参考 `LockAutoConfiguration` 的正确写法）。

### H4. 密码哈希通过 Feign 外泄
- 文件：`rosecloud-common/rosecloud-common-security/.../model/SecurityUser.java`（getter `getPassword()` 无 `@JsonIgnore`，`fromJson` 用 `@JsonProperty("password")`）、
  `rosecloud-service/rosecloud-system/.../controller/UserController.java:84-87`（`GET /system/users/auth/{username}` 无 `@PreAuthorize`，返回 `SecurityUser`）
- 现象：`SecurityUser.password` 会被 Jackson 序列化，`UserApi.loadUserByUsername` 经 Feign 返回时把 bcrypt 哈希写入响应体并在服务间传输，且可被任意已登录用户通过无鉴权的 `/auth/{username}` 拉取。
- 修复：给 `getPassword()` 加 `@JsonIgnore`；`UserController` 该端点加强 `@PreAuthorize` 或仅内部暴露，并返回不含 `password` 的投影。

### H5. 通知收件人 PII 泄露给任意已登录用户
- 文件：`rosecloud-service/rosecloud-notice/.../service/dto/MyNotice.java:8`、
  `service/impl/NoticeServiceImpl.java:225-231`（`toMyNotice`）、
  `controller/NoticeController.java:44-54`
- 现象：`MyNotice` 内嵌完整 `Notice`，含 `recipients`（每个收件人的 `email`/`phone`）以及 `targetUsername`/`targetTenantId`/`senderId`。`myNotices`/`getMine` 仅要求 `isAuthenticated()`，任何用户可枚举他人联系方式。
- 修复：返回专用只读 DTO，剔除 `recipients`（或仅保留「是否本人收件」布尔），按需裁剪 `senderId`/`targetTenantId`/`targetUsername`。

---

## 二、中危（Medium）

### M1. 多语句写入缺少 `@Transactional`，存在部分提交风险
- `UserServiceImpl.assignRoles`（`UserServiceImpl.java:119-126`）、`RoleServiceImpl.assignMenus`（`RoleServiceImpl.java:37-44`）：先删后循环插入，失败会留中间态。
- 修复：加 `@Transactional`。

### M2. 删除用户/菜单遗留孤儿关联行
- `UserServiceImpl.delete`（`UserServiceImpl.java:93-97`）+ `UserRepositoryImpl.deleteById`（`UserRepositoryImpl.java:257-260`）仅逻辑删除用户，未清理 `user_role`。
- `MenuServiceImpl.delete`（`MenuServiceImpl.java:46-53`）+ `MenuRepositoryImpl.deleteById`（`MenuRepositoryImpl.java:47-49`）未清理 `role_menu`。
- 修复：在同一事务内级联删除关联表。

### M3. 用户激活并发无乐观锁
- `UserCredentialEntity.java:20` 声明 `version` 但**未标 `@Version`**；`UserRepositoryImpl.saveActivationToken`/`confirmActivation` 设了 `version`，但 `updateById` 不加乐观锁 `WHERE`，并发 `resend` 可重复发令牌、已重发的令牌仍可被确认。
- 修复：给 `version` 加 `@Version`。

### M4. 用户名非邮箱/手机号时静默丢账号
- `UserRepositoryImpl.insert`（`UserRepositoryImpl.java:164-168`）：`email`/`phone` 仅当匹配才写入，否则皆 `null`。此类账号无法被 `loadByUsername`/`existsByUsername` 定位，永不登录且绕过唯一性约束。
- 修复：注册时归一化/强制校验为合法邮箱或手机号，或新增 `username` 列。

### M5. 激活重发接口可枚举用户并轰炸邮件
- `NoAuthController.java:88-91` → `UserActivationServiceImpl.resend`：凭用户名重发激活邮件，未知用户抛 `USER_NOT_FOUND`（枚举），可被滥用轰炸。
- 修复：未知用户也返回笼统成功；加限流/CAPTCHA/proof-of-work。

### M6. 分页拦截器硬编码 MySQL
- `RoseCloudMybatisPlusAutoConfiguration.java:33`：`new PaginationInnerInterceptor(DbType.MYSQL)` 写死；其他库分页 SQL 出错。
- 修复：DbType 可配置，默认 MYSQL。

### M7. 链路追踪 Servlet 过滤器忽略入站 `X-Trace-Id`
- `TraceContextFilter.java:34` 总是 `generateTraceId()`，未读上游网关注入的 `X-Trace-Id`（`TraceIdGlobalFilter.java:23`），分布式追踪断链。
- 修复：存在则复用入站 trace id，否则生成。

### M8. 全局异常处理吞掉校验信息且不记录日志
- `GlobalExceptionHandler.java:55-57`：`handleBadRequest` 返回 `PARAM_INVALID` 且丢弃 `ex.getMessage()`，字段级错误丢失且无日志。
- 修复：带入 `FieldError` 结构化信息并 warn 级记录。

### M9. `TenantContextTaskDecorator` 覆盖既有 `TaskDecorator`
- `TenantAutoConfiguration.java:53-64`：对**每个** `ThreadPoolTaskExecutor` 设装饰器，覆盖业务已有的 MDC/SecurityContext 传播装饰器。
- 修复：与已有装饰器组合（composite），或仅当无装饰器时设置。

### M10. `MultiTenantType.SCHEMA/DATASOURCE` 被静默忽略
- `TenantProperties.java:13` 暴露 `type`，但 `RoseCloudTenantLineHandler` 等只做列级 `tenant_id` 改写，不读 `type`；配了 `SCHEMA/DATASOURCE` 无效且误导。
- 修复：实现对应策略，或对非 `COLUMN/NONE` 类型 fail-fast/告警。

### M11. 定时发布通知无分布式锁，多副本重复投递
- `NoticeServiceImpl.java:136-153`：`@Scheduled publishDue` 在每副本独立运行，多副本会选中同一批 DRAFT 重复 `markPublished`+派发。
- 修复：加分布式锁/「PUBLISHING」状态，或单活调度。

### M12. 通知派发失败被静默吞掉，无重试
- `EmailNoticeSender.java:53-55`、`SmsNoticeSender.java:24-28`（空实现，记 success）、`NoticeDispatchService.java:51-70`（`doDispatch` 整体 try/catch，future 几乎不异常完成）。瞬时失败即丢信且无重试；SMS 无 provider 仍显示成功。
- 修复：加退避重试、按信道-收件人记录投递状态、SMS 无 provider 时显式失败/告警。

### M13. 推送类通知（ROLE/TENANT/GLOBAL）派发时未解析收件人
- `NoticeServiceImpl.java:70-77` 仅用 `request.recipients()`；`NoticeDispatchService` 也只遍历 `notice.getRecipients()`。对 ROLE/TENANT/GLOBAL 场景实际无人可投递。
- 修复：派发时按目标（角色/租户/全局）解析联系人，或显式禁止非显式收件人类型使用 EMAIL/SMS。

### M14. 会话 TTL 不一致：内存 1h vs Redis 7 天，且 refresh 提前失效
- `InMemorySessionStore.java:67-80`：按 access-token 1h 过期 `evictExpired`；`RestAwareAuthenticationSuccessHandler.java:52-55` 以 1h 设 `expireAt`，而 refresh token 有效期 24h，导致 1h 后刷新被拒。
- `RedisSessionStore.java:31,54`：固定 7 天 TTL。两者语义背离。
- 修复：按对应令牌生命周期（独立的 `refreshExpireAt`）做驱逐/过期；两实现统一基准。

### M15. 刷新/登录未撤销旧令牌，无轮换
- `RestAwareAuthenticationSuccessHandler.java:44-55`：每次成功刷新都存新会话，但从不撤销旧会话/令牌。
- 修复：刷新时先撤销上一会话再签发新对。

---

## 三、低危（Low）

- **JWT 密钥校验不一致**（`JwtTokenFactory.java:123-146`）：签名用 `new SecretKeySpec`，解析用 `Keys.hmacShaKeyFor`，密钥 <512bit 时签名成功而解析抛 `WeakKeyException`；建议两处统一并加启动校验。
- **Provider 未受检强转**（`JwtAuthenticationProvider.java:57`、`RefreshTokenAuthenticationProvider.java:57`）：`(SecurityUser) userDetails` 若非 `SecurityUser` 会抛 500，建议加类型守卫。
- **缺失安全响应头**（`SecurityConfiguration.java:78-81`）：无 HSTS、`X-Content-Type-Options`、`Referrer-Policy`、CSP；`.cacheControl().disable()` 允许令牌响应被中介缓存。
- **`BearerTokenExtractor` 大小写敏感**（`BearerTokenExtractor.java:13`）：`startsWith("Bearer ")` 不符合 RFC 6750 大小写无关，建议忽略大小写。
- **死代码/未用导入**：`RestAwareAuthenticationFailureHandler.java:21,24` 未用导入；`UserServiceImpl.updatePassword`（`UserServiceImpl.java:141-155`）与 `changePassword` 重复且未被任何控制器调用，建议删除。
- **本地缓存/锁无界**：`LocalRoseCloudCache.java:16`（`ConcurrentHashMap` 永不清退）、`LocalDistributedLock.java:16`（按 key 增长不修剪）、`RedisRoseCloudCache.increment`（`RedisRoseCloudCache.java:46-52`，INCR 与 expire 非原子）。
- **`BaseEntity` 无 `equals/hashCode`**（`BaseEntity.java:17`）：建议基于 `id` 实现。
- **`BaseData.equals` 仅比 `createdTime`**（`BaseData.java:36-51`）：等值比较基于时间戳，易误判相等，建议用真实标识。
- **存储流未关闭**（`LocalFileStorage.java:26-34`）：`Files.copy` 后未关闭调用方传入的 `InputStream`，建议 try-with-resources 或明确约定由调用方关闭。
- **分页无上下界**（`NoticeController.java:37-39,45-46` + `NoticeRepositoryImpl.java:65-66,85`）：`current`/`size` 直传 `Page`，`current<=0` 致负偏移，`size` 过大有 DoS 风险，建议 clamp `current>=1` 并限制 `size`。
- **`upsertRead/upsertConfirm` 非原子**（`NoticeRepositoryImpl.java:123-164`）：select 后 insert 无事务/唯一约束，并发会重复 `sys_notice_record` 行，建议加 `(notice_id,user_id)` 唯一索引 + `saveOrUpdate` + `@Transactional`。
- **通知发布缺入参校验**（`NoticePublishRequest.java:6-11` 无 jakarta 校验、`NoticeController.java:31` 未 `@Valid`、`NoticeServiceImpl.java:50-79` 未校验 `title/content/targetType`）；错误码误用（`NOTICE_NOT_FOUND` 用于参数错误，宜用 `PARAM_INVALID`）。
- **`TenantProfileRepositoryImpl.makeDefault`**（`TenantProfileRepositoryImpl.java:72-78`）：两次独立 `update`，并发下可能多默认或全 0，建议单条条件更新。
- **Feign 客户端无 fallback/errorDecoder**（`UserFeignApi`、`NoticePublishFeignApi`、`AuditLogFeignApi`、`LoginLogFeignApi`）：下游故障直接透传 5xx，建议加 `fallbackFactory` + 共享 `ErrorDecoder`。
- **Servlet 过滤器未开 asyncSupported**：`TraceAutoConfiguration.java:32`、`TenantAutoConfiguration.java:42` 建议 `setAsyncSupported(true)`，否则异步请求丢失 MDC/租户上下文。
- **`TenantContextHolder` / `tenant_id` 注入为 SQL 字面量**（`RoseCloudTenantLineHandler.java:27`）：建议解析时对租户 id 格式做校验/白名单。
- **审计线程池销毁生命周期**（`AuditAutoConfiguration.java:29-39`）：确认 `DelegatingSecurityContextTaskExecutor` 在上下文关闭时向内部池传播 `destroy()`，避免泄漏。

---

## 四、优先级建议

| 优先级 | 条目 |
| --- | --- |
| 立即修复（上线前阻塞） | H1、H2、H3、H4、H5 |
| 尽快修复（正确性/安全） | M1–M5、M11–M15、Low 中的 JWT 密钥与强转 |
| 跟进优化 | M6–M10、其余 Low |

> 说明：本报告所有条目均基于源码实际观察，附 `文件:行号` 定位；未做运行期验证，建议对 H/M 项补单测或集成测试确认。

---

## 五、修复记录（2026-07-10）

全部 H/M 与大部分 Low 已落地，`BUILD SUCCESS` 且全量单测通过。关键改动：

| 项 | 修复方式 | 文件 |
| --- | --- | --- |
| H1 | 网关新增 `GatewayEdgeSecurityConfig` 在边缘剥离入站 `X-Internal`，外部无法伪造内部调用 | `rosecloud-gateway/.../security/GatewayEdgeSecurityConfig.java`（新增） |
| H2 | `SecurityProperties` 默认 `publicPaths` 改为精确放行 `login/refresh/logout`；同步修正 system 的 yml | `SecurityProperties.java`、`rosecloud-system/application.yml` |
| H3 | 本地缓存 Bean 增加 `type=in-memory`（matchIfMissing）条件，Redis 后端可正确生效 | `CacheAutoConfiguration.java` |
| H4 | 改用 `@InternalApi`（`ROLE_INTERNAL` 校验）+ 网关剥离组合防护；保留 Feign 内部传密用于登录 | `UserController.java`、`InternalApiAuthenticationFilter` 注释 |
| H5 | `toMyNotice` 返回脱敏副本：剔除 `recipients`，并剥离 `targetUsername/targetTenantId/senderId` | `NoticeServiceImpl.sanitizeForRecipient` |
| M1/M2 | `delete`/`assignRoles`/`MenuServiceImpl.delete` 加 `@Transactional`；删除用户/菜单级联清理 `user_role`/`role_menu` | `UserServiceImpl`、`UserRepositoryImpl`、`MenuServiceImpl`、`MenuRepositoryImpl` |
| M3 | `UserCredentialEntity.version` 加 `@Version` 乐观锁 | `UserCredentialEntity.java` |
| M4 | `UserRepositoryImpl.insert` 校验 username 为邮箱/手机号，否则抛 `USERNAME_INVALID` | `UserRepositoryImpl.java`、`SystemErrorCode` |
| M5 | 激活重发不再抛 `USER_NOT_FOUND`（防枚举），并加按用户名冷却（默认 60s） | `UserActivationServiceImpl.java` |
| M6 | 分页拦截器 `DbType` 可配 `rosecloud.data.db-type`（默认 MYSQL） | `RoseCloudMybatisPlusAutoConfiguration.java` |
| M7 | `TraceContextFilter` 复用入站 `X-Trace-Id`；`TraceAutoConfiguration` 开 `asyncSupported` | 对应文件 |
| M8 | `handleBadRequest` 保留校验信息并 warn 记录 | `GlobalExceptionHandler.java` |
| M9 | `TenantAutoConfiguration` 用 IdentityHashMap 去重包裹，避免覆盖既有 TaskDecorator | 对应文件 |
| M10 | 启动校验租户类型，`SCHEMA/DATASOURCE` 告警 | 对应文件 |
| M11 | `markPublished` 仅更新 `DRAFT→PUBLISHED` 并返回影响行数，多副本仅赢家派发 | `NoticeRepository`/`Impl`、`NoticeServiceImpl` |
| M12 | `EmailNoticeSender` 加重试退避；`NoticeDispatchService` 失败改 error 级日志 | 对应文件 |
| M13 | `doDispatch` 对 EMAIL/SMS 但无收件人时明确告警（ROLE/TENANT/GLOBAL 收件人解析需新增 contacts API，列为已知限制） | `NoticeDispatchService.java` |
| M14/M15 | 会话 `expireAt` 统一为 refresh 有效期；`InMemorySessionStore` 的 `isRevoked/revoke` 同时匹配 access 与 refresh token；刷新时撤销旧 refresh 令牌 | `RestAwareAuthenticationSuccessHandler`、`InMemorySessionStore`、`RefreshTokenAuthenticationProvider` |
| Low | JWT 密钥签名/校验统一为同一 `SecretKeySpec` 并短密钥告警；Provider 强转加类型守卫；`BearerTokenExtractor` 大小写无关；移除死导入；补充 HSTS/CSPF/Referrer 头；`BaseEntity` 加 `equals/hashCode`；`LocalFileStorage.store` 关闭流；`LocalRoseCloudCache` 加周期清理；`RedisRoseCloudCache.increment` 仅首建设 TTL；`TenantProfileRepositoryImpl.makeDefault` 单条条件更新 | 各对应文件 |
| 测试 | `SecurityUserJsonTest` 改为校验密码经 `fromJson` 反序列化仍可还原（不出现在序列化输出需保留 Feign 传密，故以 `@InternalApi` 防护外泄）；`TraceContextFilterTest` 改为校验复用/新生成；修正 `TenantControllerTest` 中陈旧桩；移除引用不存在端点的 `auditReturnsAuditPage` | 测试文件 |
| 构建 | 为 `rosecloud-api` 增加 `jakarta.validation-api` 依赖；`TenantAutoConfiguration` 将 reactive `tenantGatewayFilter` 拆为嵌套配置，避免无网关（单体）环境类内省失败 | `rosecloud-api/pom.xml`、`TenantAutoConfiguration.java` |

### 已知限制（未实现，需新增能力或评估）
- **M13 收件人解析**：ROLE/TENANT/GLOBAL 通知的 EMAIL/SMS 派发需要按目标查询联系人的 contacts API（当前 system 服务未暴露），`sanitizeForRecipient` 已防 PII 外泄，但推送渠道在缺收件人时跳过。
- **Feign fallback**：需引入 resilience4j 等熔断依赖，本次未加。
- **`BaseData.equals`**：保持基于 `createdTime` 的现状（改为身份相等会影响既有子类值语义，风险高，暂不动）。
- **`LocalDistributedLock` 无界 map**：单实例场景下影响有限，未引入淘汰逻辑。

> 修复已通过 `./mvnw test` 全量单测（BUILD SUCCESS）。建议对 H1/H2/H4 补充集成测试（网关剥离、会话端点鉴权、内部接口越权）以覆盖运行期行为。

