# 框架使用优化审计（Framework-Usage Optimization Audit）

> 范围：在已依赖的开源框架（Spring Boot 4.1 / Spring Cloud 2025.1 / Spring Cloud Alibaba 2025.1 / MyBatis-Plus 3.5.16 / JJWT 0.12.6 / OpenFeign / Actuator / Mail / Flyway / AspectJ / Jackson）能力范围内，找出「手搓实现本可更优雅地用框架特性替代」「可简化或删除的文件」。
> 与 `ponytail-audit.md`（过度工程/死代码）互补：本文侧重**框架特性复用**与**可删除的样板层**。
> 方法：读 root `pom.xml` + 各模块 `pom.xml` 确认依赖面，再 grep 源码中的「reinventing-the-wheel」特征（线程池、`@Value`、静态 `ObjectMapper`、`toDomain/toEntity`、`ConcurrentHashMap` 缓存、`BizException` 校验、日期 API 等），并逐文件核验。

---

## 0. 依赖面（关键事实）

- `spring-boot-starter-parent 4.1.0` 已通过 transitive 提供（但**未在当前 `pom.xml` 显式声明**）：`spring-boot-starter-cache`（Caffeine）、`spring-boot-starter-validation`（Hibernate Validator / Jakarta Bean Validation）、Spring Retry、Resilience4j、WebClient / RestClient、Micrometer、虚拟线程（Java 21 + Boot 4 默认）。
- **已显式声明**：`spring-boot-starter-web`、`spring-boot-starter-security`、`spring-boot-starter-aspectj`、`spring-boot-starter-actuator`、`spring-boot-starter-mail`、`spring-boot-starter-flyway`、`spring-cloud-starter-openfeign`、`spring-cloud-starter-loadbalancer`、JJWT、MyBatis-Plus、Jackson。
- 结论：**绝大多数优化无需新增依赖**，只需把已有框架用起来。

---

## A. 可简化 / 删除的文件（最大收益）

### A1. `NoticeRepository` / `NoticeRepositoryImpl`（rosecloud-notice）— 不一致残留
- 系统模块上一轮已把 14 个 `Repository` 内联到 `BaseMapper`；但 notice 模块仍残留**唯一**的 `Repository` 抽象：
  - `rosecloud-service/rosecloud-notice/.../domain/NoticeRepository.java`
  - `rosecloud-service/rosecloud-notice/.../persistence/NoticeRepositoryImpl.java`（含 `toEntity`/`toDomain` 字段对拷 + `objectMapper.readValue(json, NOTICE_RECIPIENTS)` 手搓 JSON 解析，见第 212 行）
- 与系统模块架构保持一致：内联到 `NoticeMapper`，删除这两个文件。预计 -~200 行，并消除「同仓两套持久化范式」的不一致。

### A2. 纯 CRUD 实体的 Domain/Entity 双写层（ponytail-audit #4）— 最大「删除文件」头
- 系统模块仍有约 **25 对** `toDomain`/`toEntity` 字段对拷方法（SettingKey / SystemSetting / UserSetting / Menu / Dept / DictData / DictType / Role / LoginLog / AuditLog / TenantProfile / Tenant / User 等）。
- 其中纯 CRUD 实体（SettingKey、SystemSetting、UserSetting、Menu、Dept、DictData、DictType、Role、LoginLog、AuditLog）的 Domain 与 Entity 近乎 1:1 复制，**Domain 层可整体删除**，让 `Entity` 直接承担领域角色（Service 直接返回/接收 `Entity` 或干脆用 `record` DTO）。
- 保留有真实行为者：`Tenant`（状态机）、`User`、`TenantProfile`（JSON 扩展字段）——它们值得独立的领域类。
- 收益：削减 persistence+domain 包约 2300 行的 30–40%。与 AGENTS.md「轻量分层、不做过度抽象」「DTO/值对象优先用 record」一致。
- 若保留 Domain/Entity 双层的理由是「API 契约稳定」，见 **B3（MapStruct）** 作为替代。

### A3. `BaseData.equals/hashCode` 基于 `createdTime` — 潜在 bug
- `rosecloud-common/.../core/model/BaseData.java` 的 `equals` 仅比较 `createdTime`，`hashCode` 同理。两个**完全不同**的对象只要 `createdTime` 相同即判等。
- 当前被各子类（Tenant/User/TenantProfile 等）覆盖 `equals` 暂时掩盖；但基类实现是 footgun。
- 建议：删除基类 `equals/hashCode`（由各子类自行定义），或改为基于身份的语义。

---

## B. 用已依赖框架替代手搓（更优雅）

### B1. `TenantProfile` 静态 `new ObjectMapper()` — 反模式（ponytail-audit #11）
- `rosecloud-system/.../domain/TenantProfile.java:18`：`private static final ObjectMapper MAPPER = new ObjectMapper();`
- 领域对象里 `new ObjectMapper()` **丢失 Spring/Jackson 的全部配置**（JavaTimeModule、parameter-names 等），且每次转换依赖裸 mapper。
- 修复：把 `JsonNode` ↔ `TenantProfileData` 的转换**移出领域对象**，放到 Service / Assembler（那里已注入 Spring 的 `ObjectMapper` bean）；或在确实需要独立实例时，用 `JsonMapper.builder().findAndAddModules().build()` 构建带模块的共享实例。
- 注：当前 `BaseData` 已无静态 `ObjectMapper`（干净），仅 `TenantProfile` 有此问题。

### B2. 散落 `@Value` → `@ConfigurationProperties`（类型安全绑定）
- 项目已有先例：`SecurityProperties`、`NoticeProperties`、`TenantProperties` 都用 `@ConfigurationProperties`。
- 两处落后：
  - `UserActivationServiceImpl`（3 个 `@Value`：`rosecloud.user.activation-ttl-hours` / `activation-link-base-url` / `activation-resend-cooldown-seconds`）→ 抽成 `@ConfigurationProperties(prefix = "rosecloud.user")`。
  - `RoseCloudMybatisPlusAutoConfiguration`（`@Value("${rosecloud.data.db-type:MYSQL}")`）→ `@ConfigurationProperties(prefix = "rosecloud.data")`。
- 好处：类型安全、IDE 补全、`RelaxedBinding`、可加 `@Validated` 集中校验；与既有风格统一。

### B3. `toDomain`/`toEntity` 样板 → MapStruct（仅当保留双层时）
- 若出于 API 契约稳定而保留 Domain/Entity 双层，引入 `org.mapstruct:mapstruct`（编译期生成 mapper，零运行时反射）替代手搓字段对拷，删掉 ~25 对方法体。
- 需显式加依赖（当前未声明）。与 A2 二选一：**本项目哲学（不做过度抽象）更推荐 A2 直接合并层**。

### B4. `UserActivationServiceImpl.lastResendAt` 冷却 → Spring Cache
- `UserActivationServiceImpl.java:43`：`private final Map<String, Instant> lastResendAt = new ConcurrentHashMap<>();` 是每用户重发冷却。
- 可用 `@Cacheable` + TTL 缓存（如 `ConcurrentMapCacheManager` 或 Redis）替代：`cache key=username, value=dummy, TTL=cooldown`；命中即拒绝重发。
- 同一个类的 `globalLock` / `globalResendCount` 全局窗口（synchronized 块）是简单单实例限流，跨实例不生效（代码注释已承认）；保留或后续用 Redis 令牌桶做集群级限流。低优先级。

---

## C. 可引入的开源框架（按需，非必须）

### C1. `spring-boot-starter-validation`（Hibernate Validator / Jakarta Bean Validation）
- 当前大量 `throw new BizException(...)` 做**入参格式**校验（用户名格式、密码强度、长度等，见 `PasswordPolicyValidator`、`UserServiceImpl`、`NoticeServiceImpl` 等约 50+ 处）。
- 输入格式类校验应前移到 DTO 的 `@NotNull` / `@Size` / `@Pattern` + `@Valid`；**业务规则类**（存在性/唯一性/状态机）仍用 `BizException`。
- 需加 starter（当前未声明，但 parent BOM 已管理版本）。

### C2. 虚拟线程（Java 21 + Spring Boot 4 默认）
- `NoticeDispatchConfig` 已用 Spring `ThreadPoolTaskExecutor`（不是裸 `ThreadPoolExecutor`，做法正确）。可 `executor.setVirtualThreads(true)` 让邮件发送走虚拟线程（I/O 密集场景吞吐更高）。
- 另：Spring Boot 4 默认 `@Async` / `@Scheduled` 已可用虚拟线程，无需自管线程池。

### C3. Micrometer 自定义指标
- 已有 Actuator。暴力破解防护、激活重发计数等可走 `MeterRegistry` 暴露 Prometheus 指标，而非仅日志（便于告警/观测）。

### C4. Spring `ProblemDetail`（RFC 7807）
- 自定义 `ApiResponse` / `BizException` / `ErrorCode` 是企业惯用做法，非必须改。可逐步把错误响应迁移到 `ProblemDetail`（标准、框架原生、前端友好）。属约定变更，低优先级。

---

## D. 现代化（低风险）

### D1. `JwtTokenFactory` `java.util.Date` → `java.time.Instant` — **已核实为误报（库本身不支持，无法落地）**
- 原描述称「JJWT 0.12 原生支持 `issuedAt(Instant)` / `expiration(Instant)`」。**实测为误**：`io.jsonwebtoken`（jjwt）**从任何版本起都未给 `JwtBuilder.issuedAt/expiration/notBefore` 加过 `Instant` 重载**。已用 javap 核验 Maven Central 上全部可用版本（`0.12.5` / `0.12.6` / `0.12.7` / `0.13.0`，其中 `0.13.0` 为最新，`0.14.0` 在中央仓库不存在）——`JwtBuilder` 仅声明 `java.util.Date` 重载，`Instant-count = 0`。
- 该误报很可能把 jjwt 与 auth0 的 `java-jwt` 混淆：后者在 **v4** 才新增 `withExpiresAt(Instant)` 等 `Instant` API，而本项目用的是 `io.jsonwebtoken`（jjwt），二者不是同一库。
- 当前代码已尽量现代化：`ZonedDateTime.now().toInstant()` 作为时间源，`java.util.Date` 仅通过 `Date.from(instant)` 在 JJWT 边界做桥接，业务层未使用 `Date` 的格式化/时区等易错特性。
- **结论**：在 `jjwt`（io.jsonwebtoken）下，`Date` 无法从 JJWT 调用点消除（API 强制 `Date`）。「升级 jjwt」**也救不了 D1**（新版本同样无 `Instant` 重载）。要真正落地 D1 只能**整体换 JWT 库**（如切到 auth0 `java-jwt` v4），属重大、有风险、超出本审计范围的变更，**不推荐**。D1 按「不可行 / 前提错误」**关闭**，源码保持现状（编译态）。

### D2. `UserTenantController.readJson` 手搓解析
- `rosecloud-system/.../controller/UserTenantController.java:118` `objectMapper.readTree(value)` 可改为 `@RequestBody JsonNode` 或强类型 DTO，省去手搓异常处理。

### D3. `NoticeRepositoryImpl` JSON 字段 → MyBatis-Plus `JacksonTypeHandler`
- `NoticeRepositoryImpl.java:212` `objectMapper.readValue(json, NOTICE_RECIPIENTS)` 手搓 JSON 列读写。可用 MyBatis-Plus 的 `JacksonTypeHandler`（或 `FastjsonTypeHandler`）在 `ResultMap`/`@TableField(typeHandler=...)` 上自动序列化，消除手搓。配合 A1 内联时一并处理。

---

## 已核实为「误报 / 已修复」（无需处理）

- **ponytail #6 `BaseDataWithAdditionalInfo` 双表示**：当前源码已仅为 `JsonNode additionalInfo` 单字段（无 `byte[]` 双存储、无手搓 serde），**已是干净实现**，审计初稿描述对应的是旧版本。无需改动。
- **`NoticeDispatchConfig` 裸线程池**：实为 Spring `ThreadPoolTaskExecutor`，做法正确；仅可考虑开虚拟线程（C2）。
- **`TenantContextHolder` 的 `ThreadLocal`**：多租户上下文的惯用写法，Spring 无直接替代，保留。
- **`DeviceFingerprint` SHA-256 / `InternalApiAuthenticationFilter` 常量时间比较**：均用标准 JDK `MessageDigest`/`MessageDigest.isEqual`，正确且无框架可更优替代。
- **OpenFeign 已用于服务间调用**：未发现 `RestTemplate` / `HttpClient` / 裸 `URLConnection` 手搓 HTTP 客户端，良好。
- **D1 `JwtTokenFactory` `Date`→`Instant`**：误报（库本身不支持）。`io.jsonwebtoken`（jjwt）任何版本（`0.12.5`/`.6`/`.7`/`0.13.0`）的 `JwtBuilder` 都只有 `java.util.Date` 重载，无 `Instant`（javap 全版本核验）。「升级 jjwt」也救不了；只有整体换库（auth0 `java-jwt` v4）才行，超范围，**关闭 D1**。

---

## 推荐执行顺序（投入产出比）

1. **B2** 散落 `@Value` → `@ConfigurationProperties`（小、零风险、风格统一）。
2. **B1** `TenantProfile` 静态 `ObjectMapper` 移出（小、消除配置丢失隐患）。
3. **A1** 内联 `NoticeRepository` 并删文件（中、消除不一致）。
4. **A2** 折叠纯 CRUD 实体的 Domain 层（大、最大删码；与 A3 一并做）；或 **B3** MapStruct（若保留双层）。
5. **A3** 删 `BaseData` 危险 `equals/hashCode`（小）。
6. 其余 C / D 按需。

> 说明：以上均不改变对外 API 契约（除 A2 会合并内部层）。任何改动都应跑 `./mvnw -o -pl <module> -am test` 回归。
