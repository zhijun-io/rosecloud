# RoseCloud 技术要求说明

## 1. 文档定位

本文档只记录 `rosecloud` 的技术栈要求与工程基线，不展开产品能力、功能方案或运行验收细则。

相关的产品需求、开发计划和架构决策分别见：

- `docs/prd/product-requirements.md`
- `docs/plan/development-plan.md`
- `docs/adr/*.md`

## 2. 技术栈要求

`rosecloud` 的技术栈以“先单体、后微服务”的可演进基线为前提，当前要求如下。

### 2.1 运行时与构建工具

- JDK 21
- Maven 作为主构建工具
- 本地开发和 CI 需要支持 `./mvnw`
- 本地执行需要兼容 `sdkman` / `fnm` 这类环境管理方式

### 2.2 服务框架与分层

- Spring Boot 4.1
- Spring Cloud 2025.1
- Spring Cloud Alibaba 2025.1
- 单体入口与微服务入口并存
- 代码结构按 `common` / `api` / `starter` / `service` / `monolith` 分层

### 2.3 服务治理与通信

- Nacos 3 作为注册与配置中心
- OpenFeign 作为服务间通信主路径
- Dubbo 仅作为后续 RPC 演进预留，不作为当前主路径
- 网关统一由 Spring Cloud Gateway 承载

### 2.4 安全与身份

- Spring Security 作为安全框架
- JWT 作为默认认证方式
- OAuth2 作为后续认证增强能力
- MFA 作为后续认证增强能力

### 2.5 数据与缓存

- MySQL 作为默认业务数据库
- MyBatis-Plus 作为默认持久化框架
- Redis 作为缓存和分布式锁基础设施
- 多租户能力默认支持 COLUMN 隔离，并保留 SCHEMA / DATASOURCE 演进空间
- 多数据源能力需要保留

### 2.6 消息与调度

- RabbitMQ 作为当前消息中间件
- Kafka、ActiveMQ 作为后续消息演进选项
- P0 阶段任务调度可先采用进程内 `@Async`
- XXL-Job 作为后续调度演进选项

### 2.7 本地运行与交付

- Docker Compose 作为本地基础设施编排方式
- 单体和微服务模式都需要可本地启动、可联调、可验证
- AI 能力模块可选，默认不启用

## 3. 技术栈基线表

| 类别 | 当前基线 | 说明 |
|---|---|---|
| 运行时 | JDK 21 | 本地与 CI 均以 21 为基线 |
| 构建工具 | Maven / `./mvnw` | 多模块工程统一构建入口 |
| 应用框架 | Spring Boot 4.1 | 单体与服务端模块统一基线 |
| 微服务体系 | Spring Cloud 2025.1 | 包含网关、配置、服务治理基础 |
| 云原生扩展 | Spring Cloud Alibaba 2025.1 | 配合 Nacos 使用 |
| 注册与配置中心 | Nacos 3 | 服务注册与配置统一入口 |
| 服务通信 | OpenFeign | 当前主通信方式 |
| RPC 演进 | Dubbo | 仅作为后续演进选项 |
| 安全框架 | Spring Security | 统一认证授权入口 |
| 默认认证 | JWT | 访问态与刷新态基线方案 |
| 认证增强 | OAuth2、MFA | 作为后续能力保留 |
| 数据库 | MySQL | 默认业务数据库 |
| ORM / 持久化 | MyBatis-Plus | 默认持久化框架 |
| 多租户 | COLUMN / SCHEMA / DATASOURCE | 默认 COLUMN，保留演进空间 |
| 多数据源 | 支持 | 需要模块化封装 |
| 缓存 | Redis | 缓存与令牌/锁相关基础设施 |
| 分布式锁 | Redis | 复用缓存基础设施 |
| 网关 | Spring Cloud Gateway | 统一入口与边界控制 |
| 消息中间件 | RabbitMQ | 当前消息基线 |
| 消息演进 | Kafka、ActiveMQ | 后续可选 |
| 任务调度 | 进程内 `@Async` | P0 可用基线 |
| 调度演进 | XXL-Job | 后续可选 |
| 本地编排 | Docker Compose | 本地联调与基础设施启动 |
| AI 能力 | 可选模块 | 默认不启用 |
