# RoseCloud

`rosecloud` 是一个独立于当前仓库主工程的开发骨架。

当前阶段目标：

- 建立可独立迁移的 Maven 多模块工程
- 固化基础技术栈和模块边界
- 提供本地开发所需的基础容器编排
- 为后续逐步实现网关、认证、租户、系统管理、通知和单体模式预留稳定入口

## 当前模块

```text
rosecloud
├── pom.xml
├── docker-compose.yml
├── rosecloud-bom                 # 版本 BOM，外部消费者 import 对齐版本
├── rosecloud-common
│   ├── rosecloud-common-core
│   ├── rosecloud-common-security
│   └── rosecloud-common-web
├── rosecloud-api
├── rosecloud-starters            # 可插拔能力 starter，按需加载
│   ├── rosecloud-tenant-starter  # 多租户（rosecloud.tenant.enabled）
│   ├── rosecloud-audit-starter   # 审计（rosecloud.audit.enabled）
│   └── rosecloud-oauth2-starter  # OAuth2 JWT 资源服务器（rosecloud.oauth2.enabled）
├── rosecloud-services
│   ├── rosecloud-auth
│   ├── rosecloud-gateway
│   ├── rosecloud-notice
│   └── rosecloud-system
└── rosecloud-monolith
```

## 可插拔能力

`rosecloud-starters` 下的能力以独立 starter 提供，引入依赖不激活任何 bean，只有 `rosecloud.{name}.enabled=true` 才装配（Spring Boot `@AutoConfiguration` + `@ConditionalOnProperty`）：

| starter | 开关 | 说明 |
|---|---|---|
| rosecloud-starter-web | servlet 服务接入 | Jackson 2（替代默认 Jackson 3）+ 安全上下文过滤 + 全局异常 + Feign 头透传 |
| rosecloud-starter-data-mybatisplus | 服务按需接入 | MyBatis-Plus 持久化（可换 JPA）+ 审计自动填充 + 分页拦截器 |
| rosecloud-tenant-starter | `rosecloud.tenant.enabled` | 多租户上下文、解析器、servlet/reactive 过滤器、`@Async` 透传 |
| rosecloud-audit-starter | `rosecloud.audit.enabled` | `@AuditLog` 切面，发布 `AuditLogEvent` |
| rosecloud-oauth2-starter | `rosecloud.oauth2.enabled` | OAuth2 JWT 资源服务器，需配 `rosecloud.oauth2.jwk-set-uri` |

`rosecloud-monolith` 已引入三者并默认关闭，按需置 `enabled=true` 即激活。

## 快速开始

```bash
cd rosecloud
sdk env install
mvn clean package -DskipTests
docker compose up -d
```

按模块启动：

```bash
cd rosecloud/rosecloud-services/rosecloud-gateway
mvn spring-boot:run
```

```bash
cd rosecloud/rosecloud-services/rosecloud-auth
mvn spring-boot:run
```

```bash
cd rosecloud/rosecloud-monolith
mvn spring-boot:run
```

## 默认端口

| 模块 | 端口 |
|---|---|
| rosecloud-gateway | 9110 |
| rosecloud-auth | 9120 |
| rosecloud-system | 9130 |
| rosecloud-notice | 9150 |
| rosecloud-monolith | 9160 |

## 本地基础设施

`docker-compose.yml` 当前提供：

- MySQL
- Redis
- RabbitMQ
- Nacos 3
- XXL-Job Admin（通过 `jobs` profile 启动）

默认使用偏移端口，避免与当前 `matecloud` 本地环境直接冲突。

## 开发环境

- Java 21
- Maven 3.9+
- Docker / Docker Compose

仓库内提供了 `.sdkmanrc`，进入 `rosecloud/` 后可执行 `sdk env` 切换到推荐 JDK。

## 下一步建议

建议按下面顺序继续实现：

1. `rosecloud-common-*` 补基础返回体、异常、上下文和配置模型
2. `rosecloud-auth` 落 Spring Security + JWT 基础链路
3. `rosecloud-system` 落用户、角色、菜单、权限骨架，并承载租户开通/启停（租户管理并入 system）
4. `rosecloud-notice` 落公告、站内信和触达链路
5. `rosecloud-monolith` 作为本地单体调试入口联通上述能力
