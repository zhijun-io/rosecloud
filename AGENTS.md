# AGENTS.md

本文件为 AI 编码助手（Codex 等）在本仓库工作时提供开发规范与上下文。

## 项目概述

RoseCloud 是企业后台 + SaaS 平台底座，基于 Spring Boot 4.1 + Spring Cloud 2025.1 + Spring Cloud Alibaba 2025.1 + Nacos 3，Java 21。

- 服务间通信：OpenFeign（后续可平滑替换为 Dubbo，当前不引入）
- 认证授权：Spring Security + JWT（不使用 Sa-Token）
- 持久化：MyBatis-Plus
- 多租户：支持 COLUMN / SCHEMA / DATASOURCE，默认不启用
- 运行形态：单体（`rosecloud-monolith`）与微服务双模式
- 缓存 Redis、消息 RabbitMQ、任务调度 XXL-Job
- AI 模块可选，默认不启用

设计原则：**轻量分层、面向接口、不做过度抽象、不采用 DDD 四层强约束**。技术栈基线见 `docs/02-technical-requirements.md`，需求基线见 `docs/01-requirements.md`。

工程原则参考同作者 `rose` 项目的 `CONTRIBUTING.md`（`../rose/CONTRIBUTING.md`），下文"开发原则""文档与注释""测试""提交与协作"四节为其在 rosecloud 上的落地。

## 开发命令

```bash
sdk env install                                    # 安装 .sdkmanrc 指定的 Java 21（仅首次；Maven 经 mvnw 提供）
# 按需选择运行模式（二选一）：
task run:monolith                                  # 【单体模式】自动完成构建、基础设施准备与启动
# 或
task run:microservice                              # 【微服务模式】自动完成构建、基础设施准备与启动
task build                                         # 全量构建（经 mvnw，跳过测试）
task build:monolith                                # 构建单体模式所需模块
task build:microservice                            # 构建微服务模式所需模块
task run:monolith                                  # 单体模式启动 :9160（Docker，前台跟随日志，无其他中间件依赖）
task run:microservice                              # 微服务模式启动（Docker，网关 :9110 + auth/system/notice）
cd rosecloud-service/rosecloud-auth && ./mvnw spring-boot:run   # 运行单个服务（本机直跑，调试用）
docker compose --profile jobs up -d                # 额外启动 XXL-Job Admin
task test                                          # 构建并运行测试
task down                                          # 停止全部容器
```

选择覆盖变更的最小命令：行为改动通常 `./mvnw test` 足够；跨模块或自动配置改动用 `./mvnw verify -DskipITs`；完整校验用 `./mvnw verify`（或 `task test`）。

本地基础设施端口与凭据见 `docker-compose.yml`，默认密码 `rosecloud123`。服务端口与 matecloud 错开，避免本地共存冲突。本地 Nacos 默认关闭鉴权（`NACOS_AUTH_ENABLE=false`，匿名访问），`task run:microservice` 会自动启动微服务所需基础设施与服务；单体模式无需 Nacos/Redis/RabbitMQ，仅依赖 MySQL。

## 模块结构

```
rosecloud/
├── rosecloud-bom/                    # 版本 BOM，外部消费者 import 以对齐版本
├── rosecloud-common/
│   ├── rosecloud-common-core/        # ApiResponse、ServiceMetadata 等基础类型，零框架依赖
│   ├── rosecloud-common-security/    # SecurityHeaders、安全上下文，无 Spring Security 硬依赖
│   └── rosecloud-common-web/         # WebConstants、Web 层公共配置
├── rosecloud-api/                    # 服务间契约：Feign 接口、共享 DTO/record、枚举
├── rosecloud-starter-tech/           # 技术型 starter 父模块
│   ├── rosecloud-starter-web/        # Web 入口：Jackson 2 + 安全上下文过滤 + 全局异常 + Feign 头透传
│   ├── rosecloud-starter-security-jwt/  # JWT 编解码：access/refresh 签发与校验（auth 签发、gateway 校验）
│   ├── rosecloud-starter-data-mybatisplus/  # MyBatis-Plus 持久化（可换 JPA）
│   ├── rosecloud-starter-lock/       # 分布式锁
│   ├── rosecloud-starter-cache/      # 缓存
│   ├── rosecloud-starter-sequence/   # 业务序列号
│   ├── rosecloud-starter-storage/    # 文件存储
│   └── rosecloud-starter-oauth2/     # OAuth2 JWT 资源服务器
├── rosecloud-starter-business/       # 业务型 starter 父模块
│   ├── rosecloud-starter-tenant/     # 多租户（rosecloud.tenant.enabled）
│   └── rosecloud-starter-audit/      # 审计（rosecloud.audit.enabled）
├── rosecloud-service/
│   ├── rosecloud-gateway/            # Spring Cloud Gateway（WebFlux，9110）
│   ├── rosecloud-auth/               # 认证服务（9120）
│   ├── rosecloud-system/             # 系统管理 + 租户管理（9130）
│   └── rosecloud-notice/             # 通知中心（9150）
└── rosecloud-monolith/               # 单体模式入口，聚合上述能力（9160）
```

| 模块 | 端口 |
|---|---|
| rosecloud-gateway | 9110 |
| rosecloud-auth | 9120 |
| rosecloud-system | 9130 |
| rosecloud-notice | 9150 |
| rosecloud-monolith | 9160 |

## 可插拔 Starter

能力以独立 starter 形式提供，按需加载：每个 starter 通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册 `@AutoConfiguration`，并用 `@ConditionalOnProperty(prefix = "rosecloud.{name}", name = "enabled", havingValue = "true")` 门控——引入依赖不激活任何 bean，只有显式 `enabled=true` 才装配。

- 包根 `io.rosecloud.starter.{name}.*`；配置前缀 `rosecloud.{name}.*`；artifactId `rosecloud-{name}-starter`
- 可选第三方依赖一律 `<optional>true</optional>` 或 `provided`，避免泄漏给消费方；starter 内部 rosecloud 依赖用 `${project.version}`
- 当前 starter：
  - `rosecloud-starter-web`：servlet Web 入口，排除 Jackson 3、使用 Jackson 2（提供 `ObjectMapper` bean），并装配安全上下文过滤、全局异常处理、Feign 头透传；servlet 服务统一接入，替代 `spring-boot-starter-web`
  - `rosecloud-starter-security-jwt`：JWT（HS256）编解码——`JwtTokenCodec` 签发/校验 access、refresh，claims 与 `CurrentUser` 对齐；`@ConditionalOnClass` 引入即装配（核心认证基建，非 enabled 门控），auth 签发、gateway 校验共享同一 `rosecloud.jwt.secret`/`issuer`
  - `rosecloud-starter-data-mybatisplus`：MyBatis-Plus 持久化——`BaseEntity`（审计字段）、`AuditMetaObjectHandler` 自动填充、`MybatisPlusInterceptor`（收集 `InnerInterceptor` bean + 分页）；可整体替换为 `rosecloud-starter-data-jpa`
  - `rosecloud-starter-data-mybatisplus` 多数据源：`rosecloud.datasource.dynamic.enabled=true` 时装配 `RoseCloudRoutingDataSource`（`AbstractRoutingDataSource`，`@Primary`）为路由数据源，按 `DataSourceRoute` SPI（默认走 primary）选择目标；`rosecloud.datasource.dynamic.primary` + `datasources` 配置多目标；默认关闭，单数据源路径不受影响。路由键在事务首次取连接时解析，故请求/上下文内应保持稳定；租户级专属数据源为其扩展位
  - `rosecloud-starter-tenant`：多租户上下文（`TenantContext`）、解析器（`TenantResolver`，默认 header）、servlet/reactive 过滤器、`@Async` 透传；`rosecloud.tenant.enabled` 开启；在场 MyBatis-Plus 时经 `TenantLineInnerInterceptor`（读 `TenantContext`，无租户不隔离）做行级隔离，由 data starter 的 `MybatisPlusInterceptor` 收集
  - `rosecloud-starter-audit`：`@AuditLog` 注解 + AOP 切面，完成时发布 `AuditLogEvent`；`AuditPrincipalResolver` 可覆盖；`rosecloud.audit.enabled` 开启；默认 resolver 读 `UserContext`（操作人/租户），事件含 tenantId/target，内置日志监听器便于观察
  - `rosecloud-starter-oauth2`：OAuth2 JWT 资源服务器（servlet `SecurityFilterChain`，`@ConditionalOnMissingBean` 可覆盖）；`rosecloud.oauth2.enabled` 开启，需配 `rosecloud.oauth2.jwk-set-uri`
  - `rosecloud-starter-lock`：分布式锁抽象（`DistributedLock`，非阻塞 `tryLock` 返回 `LockToken` 或 `null`）；`rosecloud.lock.enabled` 开启，默认 `in-memory`（单实例，`ReentrantLock` 按 key），`rosecloud.lock.type=redis` 且在场 `StringRedisTemplate` 时切 Redis 后端（`SET NX PX` + Lua 释放，跨实例）；消费方需自带 `spring-boot-starter-data-redis`
  - `rosecloud-starter-cache`：缓存抽象（`RoseCloudCache`，字符串键值，不绑定序列化，消费方自行用 `ObjectMapper` 序列化）；`rosecloud.cache.enabled` 开启，默认 `in-memory`（单实例，`ConcurrentHashMap` 惰性过期），`rosecloud.cache.type=redis` 且在场 `StringRedisTemplate` 时切 Redis 后端（跨实例）；`ttl=null` 表示不过期；消费方需自带 `spring-boot-starter-data-redis`
  - `rosecloud-starter-sequence`：业务序列号生成（`SequenceGenerator`，`next(key)` 返回从 1 起的单调递增 long）；`rosecloud.sequence.enabled` 开启，默认 `in-memory`（单实例，`AtomicLong` 按 key，重启重置），`rosecloud.sequence.type=redis` 且在场 `StringRedisTemplate` 时切 Redis 后端（`INCR`，跨实例持久）；消费方需自带 `spring-boot-starter-data-redis`
  - `rosecloud-starter-storage`：文件存储抽象（`FileStorage`，按相对路径 key 存取字节流 store/load/exists/delete）；`rosecloud.storage.enabled` 开启，默认 `local`（本地文件系统，路径规范化防穿越，`rosecloud.storage.base-dir` 配置根目录）；消费方需 S3/OSS 时自定义 `FileStorage` bean 覆盖（`@ConditionalOnMissingBean` 优先）
- 版本对齐：外部消费者 import `rosecloud-bom`；内部模块用 `${project.version}`，**不在 root 导入 BOM**（import-scope BOM 无法从 reactor 解析，会阻塞首次构建）

新增 starter：在对应父模块下建 `rosecloud-starter-{name}/`。技术型 starter 继承 `rosecloud-starter-tech`，业务型 starter 继承 `rosecloud-starter-business`。写 `XxxProperties` + `@AutoConfiguration`（带 `@ConditionalOnProperty(rosecloud.{name}.enabled)`）+ `AutoConfiguration.imports`，并在对应父 POM 与 `rosecloud-bom` 注册坐标。

## 包与命名约定

- **包根**：`io.rosecloud.*`
  - 公共：`io.rosecloud.common.core.*` / `io.rosecloud.common.security.*` / `io.rosecloud.common.web.*`
  - 契约：`io.rosecloud.api.{领域}.*`（如 `io.rosecloud.api.tenant`）
  - 服务：`io.rosecloud.{服务名}.*`（如 `io.rosecloud.auth`、`io.rosecloud.tenant`）
  - 单体：`io.rosecloud.monolith.*`
- **API 前缀**：`/api/v1`（见 `ServiceMetadata.API_PREFIX` 与 `WebConstants.API_PREFIX`，新增代码引用既有常量，不要重复声明字面量）
- **统一返回体**：`ApiResponse<T>`（record：`success`、`code`、`message`、`data`），用 `ok()` / `ok(data)` / `failure(code, message)` / `failure(ErrorCode)` 构造，不要自定义其他返回结构
- **分页**：分页结果用 `ApiResponse<PageResult<T>>`，`PageResult`（`records`/`total`/`current`/`size`）
- **错误码**：`ErrorCode` 接口（`code()`/`message()`），业务异常抛 `BizException(ErrorCode)`；码格式 `{MODULE}{TYPE}{SEQ}`，模块含 `CMM`（公共）、SYS/USR/SEC/TEN/NTC 等，类型 A=参数/B=业务/E=外部；公共码见 `CommonErrorCode`
- **身份上下文**：`UserContext`（ThreadLocal）持有 `CurrentUser`，由 `rosecloud-starter-web` 的 `SecurityContextFilter` 从 `SecurityHeaders` 解码；横切链路：上游/网关透传头 → 过滤器解码为 `UserContext` → 业务读取 → 异常统一回 `ApiResponse` → Feign 调用经 `SecurityHeaderFeignPropagator` 再透传头给下游
- **DTO / 值对象**：优先用 Java `record`（如 `TenantSummary`）
- **常量类**：`final` 类 + 私有构造（如 `SecurityHeaders`、`WebConstants`），不放可变状态
- **安全上下文头**：`X-User-Id` / `X-Username` / `X-Tenant-Id` / `X-Roles` / `X-Trace-Id`（见 `SecurityHeaders`），网关解析后向下游透传

## 代码规范

代码规范已迁移到 [`CONTRIBUTING.md`](/Users/zhijunio/github/rosecloud/CONTRIBUTING.md)。

## 开发原则

- 优先复用 JDK 与既有第三方 API，再考虑新建工具类或接口；用 `lambda`、`Comparator`、`Function` 等标准抽象解决问题
- 不为预期未来变化提前引入抽象层；只有当一个接口是稳定接缝、能明显提升深度、局部性或可测试性时才新增
- 组合优于继承；通过可见性与类型让约束显式，而非仅靠注释或注解
- 尽量缩小公开面：能用 `package-private` / `final` 就不开放为 `public` 扩展点；适配类保持窄而清晰
- 模块要"深"：接口小、实现有意义；拿不准时选最简可行方案，优先标准库而非自定义抽象
- 内部实现细节用包级私有或 `.internal.` 包表达；`@Internal` 仅作为真实访问控制的补充，不能替代它

## 文档与注释

- Javadoc 用英文；类级 Javadoc 说明"如何使用该类"，而非仅描述"它是什么"
- 注释简短，聚焦意图与约束，不复述代码
- 行为、配置、兼容性或用法变化时同步更新相关文档与 Javadoc

## 测试

- 公开行为变化时在同模块新增或更新测试
- 测行为而非实现细节；优先小而可读、断言结果的测试
- 公开接缝变化时至少加一个直接覆盖该接缝的测试

## 提交与协作

- 一个提交只含一个逻辑变更；使用约定式提交前缀 `feat:` / `fix:` / `docs:` / `chore:` / `refactor:`
- 未经明确要求不要创建提交；不要推送 `main`、不要强推保护分支
- 完成前跑覆盖变更的相关 Maven 测试
- PR 描述需回答：改了什么 / 为什么 / 是否有破坏性变更（无则写 `None`）/ 如何验证

## 双模式说明

- 微服务模式：各 `rosecloud-service/*` 独立启动，经 `rosecloud-gateway` 统一入口，服务间用 OpenFeign + Nacos 发现
- 单体模式：`rosecloud-monolith` 聚合全部能力，本地联调与中小部署使用；新增业务能力需保证在两种模式下均可运行
- 单体机制：`rosecloud-monolith` 激活 `monolith` profile，组件扫描聚合 auth/system/notice（排除各服务启动类）；无网关，故由 `MonolithJwtFilter`（servlet）校验 JWT 并注入身份头，复用下游 `SecurityContextFilter`；auth→system 的 `SystemUserApi` 由 system 侧 `LocalSystemUserApi`（`@Profile("monolith")`）进程内直连，不经 Feign。单体 `@ComponentScan` 须含 `io.rosecloud.monolith`（否则 `MonolithSecurityConfiguration` 不装配）；OAuth2 关闭时（默认）由 `MonolithSecurityConfiguration` 注册一条 permitAll 的 `SecurityFilterChain`，避免 Spring Boot 默认安全链拦截全部路由，真正鉴权由 `MonolithJwtFilter` 强制执行（白名单 login/refresh/actuator，无/无效/已吊销令牌返回 401）

## 新增业务服务

手动步骤（无 CLI 脚手架）：

1. 在 `rosecloud-service/` 下建 `rosecloud-{name}/`，`pom.xml` 继承 `rosecloud-service`，按需引入 `actuator` + `web` + `openfeign` + `nacos-config` + `nacos-discovery` + `rosecloud-api` + `rosecloud-common-*`，并配 `spring-boot-maven-plugin`；若该服务需被单体聚合，`spring-boot-maven-plugin` 须配 `<classifier>exec</classifier>`（可执行胖包带 exec 分类器，主产物保持薄 jar 供单体依赖）
2. 新建 `RoseCloud{Name}Application.java`（`@SpringBootApplication` + `@EnableFeignClients`）
3. 新建 `application.yml`（端口取 9170 起的空闲段，沿用 Nacos env 占位）
4. 在 `rosecloud-service/pom.xml` 的 `<modules>` 注册新模块
5. 单体如需聚合，在 `rosecloud-monolith` 引入该服务依赖

## 参考文档

- 需求基线：`docs/01-requirements.md`
- 技术要求：`docs/02-technical-requirements.md`
- 文档索引：`docs/README.md`（仅保留需求与技术要求两类正文，不扩散中间产物）
- 工程原则来源：`../rose/CONTRIBUTING.md`
