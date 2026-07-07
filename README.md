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

`rosecloud-starters` 下的能力以独立 starter 提供，按 `@AutoConfiguration` 装配：能力型 starter（tenant/audit/oauth2）用 `rosecloud.{name}.enabled=true` 门控；核心基建 starter（`rosecloud-starter-security-jwt`）按 classpath 引入即装配：

| starter | 开关 | 说明 |
|---|---|---|
| rosecloud-starter-web | servlet 服务接入 | Jackson 2（替代默认 Jackson 3）+ 安全上下文过滤 + 全局异常 + Feign 头透传 |
| rosecloud-starter-security-jwt | 引入即装配 | JWT(HS256) access/refresh 签发与校验，claims 对齐 CurrentUser；auth 签发、gateway 校验共享 |
| rosecloud-starter-data-mybatisplus | 服务按需接入 | MyBatis-Plus 持久化（可换 JPA）+ 审计自动填充 + 分页拦截器 |
| rosecloud-tenant-starter | `rosecloud.tenant.enabled` | 多租户上下文、解析器、servlet/reactive 过滤器、`@Async` 透传、MyBatis-Plus 行级隔离（`TenantLineInnerInterceptor`） |
| rosecloud-audit-starter | `rosecloud.audit.enabled` | `@AuditLog` 切面，发布 `AuditLogEvent`（含操作人/租户，内置日志监听器） |
| rosecloud-oauth2-starter | `rosecloud.oauth2.enabled` | OAuth2 JWT 资源服务器，需配 `rosecloud.oauth2.jwk-set-uri` |

`rosecloud-monolith` 已引入三者并默认关闭，按需置 `enabled=true` 即激活。

## 快速开始

前置：Java 21、Maven 3.9+、Docker（含 Compose）。仓库根有 `.sdkmanrc`，进入目录后 `sdk env install` 可切到推荐 JDK。

```bash
git clone <repo> rosecloud && cd rosecloud
sdk env install                       # 切到 .sdkmanrc 指定的 Java 21
bash deploy/init.sh                   # 启动 MySQL/Nacos/Redis/RabbitMQ + 发布 Nacos 共享配置 + 导入建表与种子数据
mvn clean install -DskipTests         # 构建（首次需全量；脚本也会按需补构建缺失的 jar）
```

`deploy/init.sh` 幂等，克隆后执行一次即可：等待 MySQL/Nacos 就绪、向 Nacos 发布 `rosecloud-common.yaml`、导入 system/notice 的建表与种子数据（含管理员 `admin/admin123`）。

任选一种模式启动（脚本轮询就绪后打印登录信息，Ctrl-C 停止）：

```bash
bash deploy/run-monolith.sh           # 单体模式，单进程 :9160
# 或
bash deploy/run-microservice.sh       # 微服务模式，网关 :9110 + auth :9120 / system :9130 / notice :9150
```

验证登录（管理员 `admin` / `admin123`）：

```bash
BASE=http://127.0.0.1:9160            # 单体；微服务用 http://127.0.0.1:9110
TOKEN=$(curl -s -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' \
  | python3 -c 'import json,sys;print(json.load(sys.stdin)["data"]["accessToken"])')
curl -s $BASE/api/v1/system/depts/tree -H "Authorization: Bearer $TOKEN"   # 200 部门树
curl -s -X POST $BASE/api/v1/auth/logout -H "Authorization: Bearer $TOKEN" # 200
```

> 令牌吊销说明：默认 `in-memory` 吊销。单体模式登出后旧令牌立即失效（同进程）；微服务模式下 auth 与 gateway 是独立进程、吊销状态不共享，登出后旧令牌在 gateway 侧仍有效至过期。跨进程吊销需将 `rosecloud.security.token-revocation.type=redis` 并为相关服务引入 `spring-boot-starter-data-redis`（当前默认未启用，见 `docs/02-technical-requirements.md`）。

JWT 密钥默认用开发值；生产务必设置 `ROSECLOUD_JWT_SECRET`（≥32 字节，auth 与 gateway 必须一致）：

```bash
export ROSECLOUD_JWT_SECRET=change-me-please-32-bytes-minimum
```

## Nacos 共享配置

各服务通过 `spring.config.import: optional:nacos:rosecloud-common.yaml` 从 Nacos 拉取共享配置（`optional:` 表示 Nacos 不可达或配置缺失时不阻断启动）。本地起服务前需在 Nacos 创建：

- dataId：`rosecloud-common.yaml`，group：`DEFAULT_GROUP`
- 内容见 `deploy/nacos/rosecloud-common.yaml`（数据源等基础设施配置，敏感值以 `${ENV:默认值}` 引用进程环境变量，本地默认对齐 docker-compose）

auth 与 gateway 共享 JWT 密钥，启动前设置环境变量（≥32 字节）：

```bash
export ROSECLOUD_JWT_SECRET=change-me-please-32-bytes-minimum
```

本地 Nacos 默认关闭鉴权（`NACOS_AUTH_ENABLE=false`，匿名访问）；如需开启，设置 `NACOS_AUTH_ENABLE=true` 与对应 token/identity 后用 `NACOS_USERNAME` / `NACOS_PASSWORD` 覆盖。`deploy/init.sh` 已自动发布共享配置，无需手动在 Nacos 创建。Nacos 地址通过 `NACOS_SERVER_ADDR` 覆盖。

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
