# Contributing

本文件收敛 RoseCloud 的贡献规范。`AGENTS.md` 只保留仓库操作和协作约束，代码风格与模块约束以这里为准。

## 使用范围

- 代码风格、模块边界、依赖方向，以本文件为准
- 提交、测试、文档同步要求，也以本文件为准
- `AGENTS.md` 只保留仓库操作、任务命令和协作约束

## 代码规范

- **启动类**：业务服务与单体用 `@SpringBootApplication` + `@EnableFeignClients(basePackages = "io.rosecloud.api")`（显式扫描 `rosecloud-api` 的 Feign 契约；默认无 basePackages 只扫本服务包，会漏掉 `io.rosecloud.api`）；网关只用 `@SpringBootApplication`（WebFlux，不开 Feign）
- **分层**：轻量 controller / service / 持久化分层，不强制 DDD 四层；领域逻辑写在 service，不散落到 controller
- **服务间调用**：统一走 OpenFeign，Feign 接口与共享 DTO 放 `rosecloud-api`，业务服务依赖 `rosecloud-api`，不直接依赖其他服务模块。多个 `@FeignClient` 指向同一 `name`（服务）时每个须设唯一 `contextId`，否则 `FeignClientSpecification` bean 冲突；消费 `lb://` 的服务（auth/system/notice）须在 pom 引入 `spring-cloud-starter-loadbalancer`（网关已含）
- **认证**：Spring Security（`BCryptPasswordEncoder`）+ JWT。`rosecloud-starter-security-jwt` 提供 `JwtTokenCodec`（access/refresh 签发与校验，claims 与 `CurrentUser` 对齐）；`rosecloud-auth` 签发令牌，`rosecloud-gateway` 的 `JwtAuthenticationGlobalFilter` 校验 bearer 并注入 `SecurityHeaders`（先剥离客户端伪造的同名头），下游经 `rosecloud-starter-web` 的 `SecurityContextFilter` 解码为 `UserContext`，业务不重复实现。令牌密钥/issuer 走 `rosecloud.jwt.*`（env/Nacos，不入库）；令牌携带 `jti`/`exp`，登出经 `TokenRevocationService`（`rosecloud.security.token-revocation.type`）按 `jti` 吊销至过期，gateway/monolith 过滤器拒绝已吊销令牌。共享配置默认 `type=redis`（跨进程：auth 吊销写入、gateway 读取，共享 Redis）；`RedisTokenRevocationAutoConfiguration` 须 `@AutoConfiguration(before = JwtAutoConfiguration.class)` 以免 in-memory bean 遮蔽 redis。无 Redis 客户端的服务自动回退 `in-memory`（单进程内生效）——单体在 monolith pom 排除 auth 的 redis 依赖故走 in-memory。redis 后端需 `spring-boot-starter-data-redis`（auth/gateway 已引入）。登录会话记录于 `sys_login_session`，支持在线用户查询与强制下线（system 经 Feign 调 auth 吊销 `jti`），跨进程吊销/下线同样依赖 redis
- **持久化**：通过 `rosecloud-starter-data-mybatisplus` 接入 MyBatis-Plus（可整体替换为 `rosecloud-starter-data-jpa`）。约束：`rosecloud-common*` 与 `rosecloud-api` 零 ORM 依赖；service/domain 层面向 repository 接口（port），MP `BaseMapper` 与 `BaseEntity` 子类（PO）藏于各服务 infrastructure 层；换 JPA 时只替换 starter + repository 实现与 PO 基类，port 与 domain 不动。`MybatisPlusInterceptor` 由 starter 统一装配并收集所有 `InnerInterceptor` bean（如租户行级拦截）先于分页加入链
- **多租户**：隔离策略可配（默认关闭）；租户上下文通过 `X-Tenant-Id` 传递，租户开通/启停逻辑收敛在 `rosecloud-system`；开启后数据层经 `TenantLineInnerInterceptor` 按 `TenantContext` 改写 SQL（`tenant_id` 列），无租户上下文（平台/系统）不隔离。共享配置默认提供 `rosecloud.tenant.ignore-tables` 白名单，覆盖系统/通知等无 `tenant_id` 的全局表，开启与关闭模式可共存。
- **租户开通异步化**：`TenantProvisioner` 使用进程内 `@Async` 执行首个管理员创建与租户启用；不建独立任务中心，不暴露任务列表 / 重试 / 取消 / 调度配置。后续若出现新的明确异步编排需求，再单独评估是否引入任务模型。
- **通知通道**：站内为拉取式（默认）；邮件/短信为推送通道，由 `NoticeChannelSender` SPI 实现——`EmailNoticeSender`（条件装配 `JavaMailSender`，无配置则跳过）、`SmsNoticeSender`（桩）。`NoticeDispatchService` 在即时/定时发布时按 `channels` 位掩码（站内1/邮件2/短信4）经 `NoticeRecipientApi`（Feign→system）解析接收人邮箱/手机后异步分发，失败仅记日志不影响发布；接收人联系存于 `sys_user.email/phone`，按全局/租户/角色解析
- **网关路由**：动态路由由 `rosecloud-gateway` 的 `MetadataRouteDefinitionLocator` 按服务发现元数据 `rosecloud.gateway.path` 自动生成（`lb://` + Path 断言，路由 id `dyn-{serviceId}`，支持逗号分隔多路径）——新服务只需在自身 `spring.cloud.nacos.discovery.metadata.rosecloud.gateway.path` 声明路径即接入，无需改网关；`RouteRefreshScheduler` 启动时及每 30s 发布 `RefreshRoutesEvent` 刷新
- **链路标识**：网关 `TraceIdGlobalFilter` 在入口为每个请求生成/复用 `X-Trace-Id` 并向下游与响应透传（先于鉴权，白名单亦追踪）；servlet 服务经 `SecurityContextFilter` 兜底生成（直连无网关时）并写入 `traceId` MDC、回写响应头，Feign 经 `SecurityHeaderFeignPropagator` 透传；共享配置 `logging.pattern.level` 带出 `traceId` 便于日志关联
- **配置**：`application.yml` 保持精简，仅放 `spring.application.name`、`server.port`、`management` 与本地/环境专属连接项；所有可变值用 `${ENV:默认值}`，DB/Redis/MQ 等基础设施共享默认值放在 `rosecloud-common/rosecloud-common-core/src/main/resources/rosecloud-common.yaml`，各服务通过 `spring.config.import: optional:classpath:rosecloud-common.yaml` 复用。服务本地配置可以覆盖共享文件中的任意项；单体可单独覆盖 `spring.flyway.locations`、`spring.cloud.nacos.*` 和单体开关。
- **不引入**：Sa-Token（用 Spring Security）、Dubbo（用 OpenFeign，待后续演进）、Swagger/Smart-Doc（API 文档方案未定，暂不引入）
- **Jackson**：统一用 Jackson 2（`com.fasterxml.jackson`），不用 Spring Boot 4 默认的 Jackson 3（`tools.jackson`）。servlet 服务通过 `rosecloud-starter-web` 接入（已排除 `spring-boot-starter-jackson` 并提供 Jackson 2 `ObjectMapper`），不要直接依赖 `spring-boot-starter-web`/`spring-boot-starter-jackson`；reactive 网关暂仍为 Jackson 3（codec 切换待跟进）。注意：OpenFeign 的 `FeignJacksonConfiguration`（`PageJacksonModule` 继承 Jackson 3 的 `tools.jackson.databind.JacksonModule`）在 Feign 服务类路径出现 `spring-data-commons`（如引入 redis）时被 `@ConditionalOnClass(Page,Sort)` 激活并加载，仅 Jackson 2 下抛 `ClassNotFoundException`；故各 Feign 服务 `application.yml` 设 `spring.cloud.openfeign.autoconfiguration.jackson.enabled=false`（rosecloud 用自有 `PageResult`，不需要该模块）
- **依赖方向**：`common-core` 为最底层零依赖；`common-security`、`common-web` 依赖 `common-core`；`api` 依赖 `common-core` + `common-security`；服务依赖 `api` + 所需 common；禁止反向依赖与 common 之间的循环

## 文档同步

- 行为、配置、兼容性或用法变化时，先更新文档再合并代码
- 公开命令、启动方式、配置前缀变化时，至少同步 `README.md` 和 `AGENTS.md`
- 对外可见的约定变化，优先写进 `docs/` 中的需求或技术要求文档

## 测试要求

- 公开行为变化时，在同模块新增或更新测试
- 测行为，不测实现细节；优先小而可读、断言结果的测试
- 公开接缝变化时，至少补一个直接覆盖该接缝的测试
- 完成前跑覆盖变更的相关 Maven 测试

## 提交与协作

- 一个提交只含一个逻辑变更；使用约定式提交前缀 `feat:` / `fix:` / `docs:` / `chore:` / `refactor:`
- 未经明确要求不要创建提交；不要推送 `main`，不要强推保护分支
- PR 描述需回答：改了什么、为什么、是否有破坏性变更、如何验证
