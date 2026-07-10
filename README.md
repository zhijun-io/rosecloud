# RoseCloud

`rosecloud` 是一个独立于当前仓库主工程的开发骨架。

仓库定位：

- 提供可独立迁移的 Maven 多模块工程
- 固化基础技术栈和模块边界
- 提供本地开发所需的基础容器编排
- 为后续逐步实现网关、认证、租户、系统管理、通知和单体模式预留入口

## 当前模块

```text
rosecloud
├── pom.xml
├── docker-compose.yml
├── rosecloud-bom                 # 版本 BOM，外部消费者 import 对齐版本
├── rosecloud-common
│   ├── rosecloud-common-core
│   └── rosecloud-common-security
├── rosecloud-api
├── rosecloud-starter        # 技术型 starter 父模块
│   ├── rosecloud-starter-web
│   ├── rosecloud-starter-security
│   ├── rosecloud-starter-trace
│   ├── rosecloud-starter-data-mybatisplus
│   └── rosecloud-starter-cache
├── rosecloud-starter-business    # 业务型 starter 父模块
│   ├── rosecloud-starter-tenant  # 多租户（rosecloud.tenant.enabled）
│   └── rosecloud-starter-audit   # 审计（rosecloud.audit.enabled）
├── rosecloud-service
│   ├── rosecloud-auth
│   ├── rosecloud-gateway
│   ├── rosecloud-notice
│   └── rosecloud-system
└── rosecloud-monolith
```

## 可插拔能力

starter 分成两个父模块：`rosecloud-starter` 承载技术型 starter，`rosecloud-starter-business` 承载业务型 starter。二者都通过 `@AutoConfiguration` 装配；业务型 starter（tenant/audit）用 `rosecloud.{name}.enabled=true` 门控，核心基建 starter（`rosecloud-starter-security`、`rosecloud-starter-trace`）按 classpath 引入即装配：

| starter | 开关 | 说明 |
|---|---|---|
| rosecloud-starter-web | servlet 服务接入 | Jackson 2（替代默认 Jackson 3）+ 全局异常 |
| rosecloud-starter-security | 引入即装配 | JWT(HS256) access/refresh 签发与校验，claims 对齐 CurrentUser；安全上下文与 Feign 透传；OAuth2 JWT 资源服务器；预留 MFA hooks |
| rosecloud-starter-trace | 引入即装配 | 基于 Micrometer Tracing（Brave 桥），由 Spring Boot 自动装配服务端追踪；日志 MDC 注入 `traceId`/`spanId`，跨服务经 W3C `traceparent` 透传。后续接入 Zipkin/OTel 仅需补一个 reporter 依赖 |
| rosecloud-starter-data-mybatisplus | 服务按需接入 | MyBatis-Plus 持久化（可换 JPA）+ 审计自动填充 + 分页拦截器 |
| rosecloud-starter-tenant | `rosecloud.tenant.enabled` | 多租户上下文、解析器、servlet/reactive 过滤器、`@Async` 透传、MyBatis-Plus 行级隔离（`TenantLineInnerInterceptor`） |
| rosecloud-starter-audit | `rosecloud.audit.enabled` | `@AuditLog` 切面，发布 `AuditLogEvent`（含操作人/租户，内置日志监听器） |

`rosecloud-monolith` 引入了业务型 starter 和需要的技术型 starter，并默认启用单体内联 wiring，不需要额外的 `monolith` profile 或开关；可选能力仍按需置 `enabled=true` 即激活。

## 快速开始

前置：[sdkman](https://sdkman.io)（已安装配置）、Docker（含 Compose）、[task](https://taskfile.dev)（`brew install go-task/tap/go-task`）。仓库根 `.sdkmanrc` 锁定 Java 21，首次 `sdk env install` 装 JDK；Maven 经 `mvnw` 包装器提供，无需单独安装。

```bash
git clone <repo> rosecloud && cd rosecloud
sdk env install          # 安装 .sdkmanrc 指定的 Java 21（仅首次）
# 按需选择运行模式（二选一）：
task run:monolith        # 【单体模式】自动完成构建与启动
# 或
task run:microservice    # 【微服务模式】自动完成构建、基础设施启动与服务启动
task build               # 全量构建（经 mvnw，跳过测试）
```

`task run:monolith` / `task run:microservice` 由 `Taskfile.yml` 编排，会自动完成构建、基础设施准备和服务启动：
- 单体模式仅依赖 MySQL，无需其他中间件，令牌吊销用内存存储
- 微服务模式启动全套基础设施，包含服务发现、配置中心、缓存、消息队列等能力

任选对应模式启动（服务以 Docker 容器运行，前台跟随日志，Ctrl-C 停止）：

```bash
task run:monolith        # 单体模式，单进程 :8080（无其他中间件依赖）
# 或
task run:microservice    # 微服务模式，网关 :8080 + auth :9090 / system :9110 / notice :9120
```

验证登录（管理员 `admin` / `admin123`）：

```bash
BASE=http://127.0.0.1:8080            # 单体与微服务同用 8080（两种模式互斥），测试脚本用 ROSECLOUD_MODE 区分
TOKEN=$(curl -s -X POST $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' \
  | python3 -c 'import json,sys;print(json.load(sys.stdin)["data"]["accessToken"])')
  
curl -s $BASE/api/system/depts/tree -H "Authorization: Bearer $TOKEN"   # 200 部门树
curl -s -X POST $BASE/api/auth/logout -H "Authorization: Bearer $TOKEN" # 200
```

> 令牌吊销说明：共享配置默认 `type=redis`（auth/gateway 共享 Redis）。微服务模式登出后旧令牌在 gateway 侧即时失效（跨进程：auth 写入 Redis、gateway 读取校验）；单体模式无 Redis 依赖、回退 `in-memory`，登出后同进程即时失效。无 Redis 环境可设 `ROSECLOUD_TOKEN_REVOCATION_TYPE=in-memory`。

## 共享配置

各服务通过 `spring.config.import` 复用共享配置（`optional:` 表示共享文件不存在时不阻断启动）。共享文件位于 `rosecloud-common/rosecloud-common-core/src/main/resources/rosecloud-common.yaml`。

导入顺序为 `classpath` 默认值 -> Nacos 公共配置 -> Nacos 服务配置 -> Nacos profile 配置（`dev/test/prod`），因此服务自己的 `application.yml` 和 Nacos 中的服务/profile 配置都可以覆盖共享文件中的任意项；单体模式同样适用，适合保留自己的 `spring.flyway.locations`、`spring.cloud.nacos.*` 和单体专属配置。

## 许可证

本仓库采用 [Apache License 2.0](LICENSE)。

## 变更记录

见 [CHANGELOG.md](CHANGELOG.md)。

## 默认端口

| 模块 | 端口 |
|---|---|
| rosecloud-gateway | 8080 |
| rosecloud-auth | 9090 |
| rosecloud-system | 9110 |
| rosecloud-notice | 9120 |
| rosecloud-monolith | 8080 |

## 本地基础设施

`docker-compose.yml` 当前提供：

- MySQL
- Redis
- RabbitMQ
- Nacos 3
- XXL-Job Admin（通过 `jobs` profile 启动）

默认使用偏移端口，避免与当前 `matecloud` 本地环境直接冲突。

## 密钥与安全

本地编排的密钥与账号是**本地起站默认值**，不是生产密钥。`docker-compose.yml` 里保留了便于本地开发的默认口令、JWT secret 和内部调用密钥，生产环境必须通过 `.env` 或外部配置覆盖。

- **生产部署**：必须把这些密钥通过 `.env`（已被 `.gitignore` 忽略）覆盖；变量清单与取值建议见仓库根 `.env.example`。
- **Nacos 本地默认关闭鉴权**（`NACOS_AUTH_ENABLE=false`）。如果要验证鉴权链路，请在 `.env` 中显式设置 `NACOS_AUTH_ENABLE=true`，再用 `NACOS_USERNAME` / `NACOS_PASSWORD` 提供独立强凭证：

  ```bash
  # 1) 用默认账号登录拿到 token
  TOKEN=$(curl -s -X POST 'http://127.0.0.1:8848/nacos/v1/auth/login' \
    -d 'username=nacos&password=nacos' | jq -r .accessToken)
  # 2) 修改 nacos 管理员密码
  curl -X PUT "http://127.0.0.1:8848/nacos/v1/auth/users?accessToken=$TOKEN" \
    -d "username=nacos&newPassword=<你的强密码>"
  # 3) 在 .env 中设置 NACOS_USERNAME=nacos / NACOS_PASSWORD=<你的强密码> 后重建服务
  ```

- **已有本地数据卷注意**：如果你本地已经用旧密码初始化过数据卷，而当前 `docker-compose.yml` 的默认值又和你的现有卷不一致，启动后会连接失败。两种处理：
  1. 在 `.env` 里显式写回与你本地卷一致的密码，保留现有数据；
  2. 或 `docker compose down -v` 销毁卷后重新初始化（清空本地数据）。

## 开发环境

- [sdkman](https://sdkman.io) + `.sdkmanrc`（锁定 Java 21）
- Maven 经 `mvnw` 包装器提供（版本见 `.mvn/wrapper/`，无需单独安装）
- Docker / Docker Compose
- [task](https://taskfile.dev)（`brew install go-task/tap/go-task`）

进入 `rosecloud/` 后 `sdk env` 切到 Java 21；`task --list` 查看全部命令，服务统一以 Docker 容器运行。
