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
sdk env install                       # 切到 .sdkmanrc 指定的 Java 21
mvn clean install -DskipTests         # 全量构建
mvn clean install -pl rosecloud-services/rosecloud-auth -am -DskipTests   # 构建单模块及其依赖
cd rosecloud-services/rosecloud-auth && mvn spring-boot:run               # 运行单个服务
docker compose up -d                  # 启动本地 MySQL/Redis/RabbitMQ/Nacos
docker compose --profile jobs up -d   # 额外启动 XXL-Job Admin
mvn test                              # 单元测试
```

选择覆盖变更的最小命令：行为改动通常 `mvn test` 足够；跨模块或自动配置改动用 `mvn verify -DskipITs`；完整校验用 `mvn verify`。

本地基础设施端口与凭据见 `docker-compose.yml`，默认密码 `rosecloud123`。服务端口与 matecloud 错开，避免本地共存冲突。

## 模块结构

```
rosecloud/
├── rosecloud-bom/                    # 版本 BOM，外部消费者 import 以对齐版本
├── rosecloud-common/
│   ├── rosecloud-common-core/        # ApiResponse、ServiceMetadata 等基础类型，零框架依赖
│   ├── rosecloud-common-security/    # SecurityHeaders、安全上下文，无 Spring Security 硬依赖
│   └── rosecloud-common-web/         # WebConstants、Web 层公共配置
├── rosecloud-api/                    # 服务间契约：Feign 接口、共享 DTO/record、枚举
├── rosecloud-starters/               # 可插拔能力 starter，按需加载（见下）
│   ├── rosecloud-tenant-starter/     # 多租户（rosecloud.tenant.enabled）
│   ├── rosecloud-audit-starter/      # 审计（rosecloud.audit.enabled）
│   └── rosecloud-oauth2-starter/     # OAuth2 JWT 资源服务器（rosecloud.oauth2.enabled）
├── rosecloud-services/
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
  - `rosecloud-tenant-starter`：多租户上下文（`TenantContext`）、解析器（`TenantResolver`，默认 header）、servlet/reactive 过滤器、`@Async` 透传；`rosecloud.tenant.enabled` 开启
  - `rosecloud-audit-starter`：`@AuditLog` 注解 + AOP 切面，完成时发布 `AuditLogEvent`；`AuditPrincipalResolver` 可覆盖；`rosecloud.audit.enabled` 开启
  - `rosecloud-oauth2-starter`：OAuth2 JWT 资源服务器（servlet `SecurityFilterChain`，`@ConditionalOnMissingBean` 可覆盖）；`rosecloud.oauth2.enabled` 开启，需配 `rosecloud.oauth2.jwk-set-uri`
- 版本对齐：外部消费者 import `rosecloud-bom`；内部模块用 `${project.version}`，**不在 root 导入 BOM**（import-scope BOM 无法从 reactor 解析，会阻塞首次构建）

新增 starter：在 `rosecloud-starters/` 下建 `rosecloud-{name}-starter/`（继承 `rosecloud-starters`），写 `XxxProperties` + `@AutoConfiguration`（带 `@ConditionalOnProperty(rosecloud.{name}.enabled)`）+ `AutoConfiguration.imports`，并在 `rosecloud-starters/pom.xml` 与 `rosecloud-bom` 注册坐标。

## 包与命名约定

- **包根**：`io.rosecloud.*`
  - 公共：`io.rosecloud.common.core.*` / `io.rosecloud.common.security.*` / `io.rosecloud.common.web.*`
  - 契约：`io.rosecloud.api.{领域}.*`（如 `io.rosecloud.api.tenant`）
  - 服务：`io.rosecloud.{服务名}.*`（如 `io.rosecloud.auth`、`io.rosecloud.tenant`）
  - 单体：`io.rosecloud.monolith.*`
- **API 前缀**：`/api/v1`（见 `ServiceMetadata.API_PREFIX` 与 `WebConstants.API_PREFIX`，新增代码引用既有常量，不要重复声明字面量）
- **统一返回体**：`ApiResponse<T>`（record：`success`、`code`、`message`、`data`），用 `ok()` / `ok(data)` / `failure(code, message)` 构造，不要自定义其他返回结构
- **DTO / 值对象**：优先用 Java `record`（如 `TenantSummary`）
- **常量类**：`final` 类 + 私有构造（如 `SecurityHeaders`、`WebConstants`），不放可变状态
- **安全上下文头**：`X-User-Id` / `X-Username` / `X-Tenant-Id` / `X-Roles` / `X-Trace-Id`（见 `SecurityHeaders`），网关解析后向下游透传

## 代码规范

- **启动类**：业务服务与单体用 `@SpringBootApplication` + `@EnableFeignClients`；网关只用 `@SpringBootApplication`（WebFlux，不开 Feign）
- **分层**：轻量 controller / service / 持久化分层，不强制 DDD 四层；领域逻辑写在 service，不散落到 controller
- **服务间调用**：统一走 OpenFeign，Feign 接口与共享 DTO 放 `rosecloud-api`，业务服务依赖 `rosecloud-api`，不直接依赖其他服务模块
- **认证**：Spring Security + JWT；鉴权过滤、token 解析放 `rosecloud-common-security`，业务服务通过 `SecurityHeaders` 读取身份，不重复实现
- **持久化**：当前选 MyBatis-Plus，但须保持可替换为 JPA。约束：`rosecloud-common*` 与 `rosecloud-api` 零 ORM 依赖；MP 仅存在于各服务 infrastructure 层，service 面向 repository 接口（port），`BaseMapper` 作为实现藏其后；租户数据隔离不绑死 MP，走 `rosecloud-tenant-starter` 的持久化无关 SPI + 可选 MP adapter，换 JPA 时只替换 adapter 与 repository 实现
- **多租户**：隔离策略可配（默认关闭）；租户上下文通过 `X-Tenant-Id` 传递，租户开通/启停逻辑收敛在 `rosecloud-system`
- **配置**：`application.yml` 保持精简，仅放 `spring.application.name`、`server.port`、`management` 与 Nacos 连接；所有可变值用 `${ENV:默认值}`，DB/Redis/MQ 等基础设施配置走 Nacos 共享配置
- **不引入**：Sa-Token（用 Spring Security）、Dubbo（用 OpenFeign，待后续演进）、Swagger/Smart-Doc（API 文档方案未定，暂不引入）
- **依赖方向**：`common-core` 为最底层零依赖；`common-security`、`common-web` 依赖 `common-core`；`api` 依赖 `common-core` + `common-security`；服务依赖 `api` + 所需 common；禁止反向依赖与 common 之间的循环

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

- 微服务模式：各 `rosecloud-services/*` 独立启动，经 `rosecloud-gateway` 统一入口，服务间用 OpenFeign + Nacos 发现
- 单体模式：`rosecloud-monolith` 聚合全部能力，本地联调与中小部署使用；新增业务能力需保证在两种模式下均可运行

## 新增业务服务

手动步骤（无 CLI 脚手架）：

1. 在 `rosecloud-services/` 下建 `rosecloud-{name}/`，`pom.xml` 继承 `rosecloud-services`，按需引入 `actuator` + `web` + `openfeign` + `nacos-config` + `nacos-discovery` + `rosecloud-api` + `rosecloud-common-*`，并配 `spring-boot-maven-plugin`
2. 新建 `RoseCloud{Name}Application.java`（`@SpringBootApplication` + `@EnableFeignClients`）
3. 新建 `application.yml`（端口取 9170 起的空闲段，沿用 Nacos env 占位）
4. 在 `rosecloud-services/pom.xml` 的 `<modules>` 注册新模块
5. 单体如需聚合，在 `rosecloud-monolith` 引入该服务依赖

## 参考文档

- 需求基线：`docs/01-requirements.md`
- 技术要求：`docs/02-technical-requirements.md`
- 文档索引：`docs/README.md`（仅保留需求与技术要求两类正文，不扩散中间产物）
- 工程原则来源：`../rose/CONTRIBUTING.md`
