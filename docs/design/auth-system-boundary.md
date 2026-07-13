# Auth 与 System 服务边界约定

> 状态：约定（v1.0）
> 触发：MFA 设计时发现认证域数据（MFA 配置）若复用 `sys_user_setting`/`sys_system_setting`，会让 `rosecloud-auth` 或 `rosecloud-starter-security` 反向依赖 `rosecloud-system`，暴露边界未定义的问题。
> 适用：`rosecloud-auth`、`rosecloud-system` 两个微服务及其与 `rosecloud-starter-security` 的关系。

## 1. 依赖方向（已落地，须维持）

- `rosecloud-auth` 与 `rosecloud-system` 是**兄弟服务**，**互不编译依赖**：
  - `rosecloud-system` pom 依赖：`rosecloud-api`、`rosecloud-common-core`、`rosecloud-common-security`、`rosecloud-starter-security`（库）、`rosecloud-starter-web/data-mybatisplus/audit/trace`。**不依赖 `rosecloud-auth`**。
  - `rosecloud-auth` pom 依赖：`rosecloud-api`、`rosecloud-common-core`、`rosecloud-common-security`、`rosecloud-starter-security`（库）、`rosecloud-starter-web/trace`。**不依赖 `rosecloud-system`**。
- 二者通过**共享库 + 契约层**协作：`rosecloud-starter-security`（认证逻辑库）、`rosecloud-api`（DTO/契约）、`rosecloud-common-*`（基础类型）。
- 跨服务取数一律经 `rosecloud-api` 的 DTO + OpenFeign，**显式、按需、只读**，不形成环。

## 2. `rosecloud-starter-security` 的定位

- 它是 **starter / 库**，承载认证**逻辑**：JWT 工厂与资源服务器、`PRE_AUTH`/`MFA_CONFIGURATION` 等作用域、MFA 流程编排。
- **不依赖任何微服务模块、不直接访问 DB**。需要持久化时**声明接口**（如 `MfaConfigStore`），由 `rosecloud-auth` 服务实现并落库。
- 它可以被 `rosecloud-system` 等任意服务引入（用于资源服务器/鉴权），但引入方不会因此获得数据库耦合。

## 3. 职责划分

| 维度 | `rosecloud-auth`（身份与访问） | `rosecloud-system`（管理与资源） |
| --- | --- | --- |
| 登录/登出、凭证校验 | ✅（密码、OTP 经 notice） | — |
| JWT 签发/校验、会话/刷新 | ✅（`PRE_AUTH`/`MFA_CONFIGURATION` 等） | — |
| MFA（逻辑 + 自有表） | ✅ `auth_user_mfa` / `auth_mfa_settings` | — |
| 登录失败锁定、限频（认证侧执行） | ✅ | 定义/参数可存 system 配置 |
| 登录日志（认证视角） | ✅ | — |
| 用户 CRUD / 档案、状态 | — | ✅ |
| 租户、角色、权限菜单目录 | — | ✅ |
| 通用配置管理 KV | — | ✅ `sys_user_setting` / `sys_system_setting` |
| 密码复杂度/有效期/账户过期（定义与存储） | 读取执行 | ✅ 定义与存储 |
| 任务/调度、审计目录、字典 | — | ✅ |

## 4. 边界规则（硬性）

1. **互不编译依赖**：auth 不依赖 system，system 不依赖 auth。新增依赖须先走本约定评审。
2. **认证域数据自持**：凡是「凭证 / 令牌 / 会话 / MFA / 登录策略」相关数据，**由 auth 自有表承载**，不得写入 `sys_user_setting`/`sys_system_setting`。
3. **配置 KV 归 system 且通用**：`sys_*` 仅承载非认证域的偏好/开关（如 UI 偏好、功能开关）。auth 不借用它存认证数据。
4. **库与服务的分离**：`rosecloud-starter-security` 保持微服务无关；任何 DB 访问只能发生在 `rosecloud-auth` 服务内，经 starter 声明的接口注入。
5. **划分判据**：数据属于「认证/授权/凭证/会话」→ auth；属于「用户档案/租户/角色/通用偏好/调度」→ system。存疑时，认证相关数据跟随 auth 自持，不借用 system 的 KV。

## 5. 反例（MFA）

- 错误：让 `rosecloud-starter-security`（或 auth）经 `UserSettingService`/`SystemSettingService` 读写 `sys_user_setting`/`sys_system_setting` —— 等价于 auth/库反向依赖 `rosecloud-system`。
- 正确：starter 声明 `MfaConfigStore` 接口，auth 实现并落到 `auth_user_mfa`/`auth_mfa_settings`；starter 经接口使用，零微服务耦合。

## 6. 相关

- `docs/design/mfa.md` —— 落地本约定的认证域数据自持示例。
- `docs/adr/configuration-model-decision.md` —— 配置模型决策（KV vs 强类型）。
- `docs/adr/thingsboard-reference.md` —— MFA 设计参考来源。
- **已落地**：登录日志 `sys_login_log` → `auth_login_log`，实体/映射/服务/控制器与表随 `rosecloud-auth` 迁移；`LoginLogFeignApi` 改指 `rosecloud-auth`（`/api/auth/login-logs`），auth 内实现标记 `@Primary` 以消歧；system 仅经 Feign 上报。
