# AGENTS.md

本文件为 AI 编码助手在本仓库工作时提供操作上下文、约束与常用命令。

## Context

RoseCloud 是一个企业后台 + SaaS 平台底座，基于 Spring Boot 4.1、Spring Cloud 2025.1、Spring Cloud Alibaba 2025.1、Nacos 3 和 Java 21。

- 服务间通信使用 OpenFeign
- 认证授权使用 Spring Security + JWT，统一收口到 `rosecloud-starter-security`，其内承载 JWT、OAuth2 JWT 资源服务器和后续 MFA hooks
- 持久化使用 MyBatis-Plus
- 运行形态分为单体 `rosecloud-monolith` 和微服务 `rosecloud-service/*`
- 共享技术模块分为 `rosecloud-common/`、`rosecloud-api/`、`rosecloud-starter/`、`rosecloud-starter-business/`

设计原则是轻量分层、面向接口、不做过度抽象。更完整的需求和技术背景见 `docs/prd/product-requirements.md`、`docs/prd/technical-requirements.md`。

## Structure

| Path | Purpose |
| --- | --- |
| `pom.xml` | 根聚合与依赖管理 |
| `rosecloud-bom/` | 外部消费者使用的版本 BOM |
| `rosecloud-common/` | 公共基础类型、security/web 共享能力 |
| `rosecloud-api/` | 服务间契约、DTO、record、枚举 |
| `rosecloud-starter/` | 技术型 starter 聚合 |
| `rosecloud-starter-business/` | 业务型 starter 聚合 |
| `rosecloud-service/` | gateway/auth/system/notice 服务 |
| `rosecloud-monolith/` | 单体入口 |
| `docs/` | 需求、技术要求、参考文档 |
| `Taskfile.yml` | 本地构建与运行编排 |
| `docker-compose.yml` | 本地基础设施与服务容器编排 |

## Commands

```bash
sdk env install
task run:monolith
task run:microservice
task build
task build:monolith
task build:microservice
task test
task down
cd rosecloud-service/rosecloud-auth && ./mvnw spring-boot:run
```

- 对行为变更，优先跑受影响模块的最小 Maven 测试。
- 跨模块或自动配置变更，用 `./mvnw verify -DskipITs`。
- 完整校验用 `./mvnw verify` 或 `task test`。
- 本地 Java 版本必须是 21；如果 shell 默认不是 21，先切到 21 再跑 Maven。

## Conventions

- 包根使用 `io.rosecloud.*`
- API 前缀统一使用 `ServiceMetadata.API_PREFIX`
- 统一返回体使用 `ApiResponse<T>`
- 分页统一使用 `ApiResponse<PageResult<T>>`
- 错误码使用 `ErrorCode`，业务异常使用 `BizException`
- 常量类使用 `final` + 私有构造
- 不可变数据载体与 DTO 的注解选型见 [docs/lombok-conventions.md](docs/lombok-conventions.md)。简则：纯 DTO / 不实现接口的值对象用 `record`；**实现 `HasId`/`HasTenantId`/`HasUserId`/`HasStatus` 等 JavaBean 接口的不可变载体必须用 `@Value`**（不能用 `record`）；Entity / 可变 POJO 用 `@Getter @Setter @NoArgsConstructor`。
- starter 命名使用 `rosecloud-{name}-starter`
- 新 starter 通过 `AutoConfiguration.imports` 注册
- 对外可见变化时同步更新相关测试和文档

## Commit & PR

- 一个提交只包含一个逻辑变更
- 提交信息使用约定式前缀，如 `feat:`、`fix:`、`docs:`、`refactor:`、`chore:`
- PR 描述要回答 4 件事：改了什么、为什么、是否有破坏性变更、如何验证

## Agent notes

- 不要随意扩大公开 API 面，优先复用现有常量和契约。
- 不要改动与当前任务无关的未请求文件。
- 只在确有必要时新增子目录 `AGENTS.md`；当前仓库没有需要单独覆盖的子目录规则。
- 详细贡献规范见 `CONTRIBUTING.md`，用户文档见 `README.md`。
