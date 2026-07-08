# RoseCloud Security Starter —— Thingsboard 借鉴分析（PRD/TR 需求对齐版）

## 1. 总体架构对比

| 维度 | Thingsboard | RoseCloud 本期目标 |
|------|-----------|-----------------|
| 框架 | Spring Security (servlet) | 同，自动装配 starter |
| 会话 | 无状态 | 同 |
| 认证入口 | Rest + JWT + Refresh + API Key | Rest + JWT + Refresh（无 OAuth2/MFA/API Key） |
| 凭证载体 | `UserPrincipal(Type.USER_NAME\|PUBLIC_ID, value)` | 直接借鉴 |
| 安全用户 | `SecurityUser extends User` | `SecurityUser` 组合 rosecloud `User`（`User` 是 final） |
| JWT 签名 | HS512, JJWT | 同 |
| 接入点 | `UserService` + `UserAuthDetailsCache` | **`UserDetailsService` 为唯一必需接入点** |
| JWT claims | 丰富（firstName, lastName, enabled, tenantId, customerId…） | **最小化**：username + jti + exp（PRD §4.12） |
| 角色体系 | Authority 枚举（SYS_ADMIN/TENANT_ADMIN/CUSTOMER_USER） | UserDetailsService.authorities 动态获取（ROLE_xxx + PERM_xxx） |
| RBAC | Resource/Operation → PermissionChecker → AccessControlService | P0：`@PreAuthorize` + `hasRole/hasAuthority`；Resource/Operation 枚举预留 |
| 租户登录 | 无概念（email 全局唯一） | **核心差异**：普通用户需租户标识，平台管理员无需（PRD §4.6） |
| 租户上下文 | JWT claims → tenantId | Gateway 注入 header → 全链路透传（TR §6.1） |
| 会话管理 | 无 | P0 必需：在线查询 + 强制下线 + jti 吊销（PRD §4.12） |

## 2. 核心模型借鉴

### 2.1 SecurityUser

Thingsboard 版（继承）：
```java
public class SecurityUser extends User {
    private Collection<GrantedAuthority> authorities;
    private boolean enabled;
    private UserPrincipal userPrincipal;
    private String sessionId = UUID.randomUUID().toString();
}
```

**RoseCloud 适配：**
- rosecloud `User` 是 `final`，不能继承 → **组合模式**
- 实现 `UserDetails`，可直接放入 `SecurityContextHolder.getContext().getAuthentication()`
- 内部持有 `User` 域字段（`id`/`username`/`nickname`/`tenantId`），额外字段来自 `UserDetailsService`
- `enabled` = `status == 1`
- `getAuthorities()` 从 `UserDetailsService` 返回的 `GrantedAuthority` 列表获取（`ROLE_xxx` + `PERM_xxx`）
- `getPassword()` 返回 `null`：JWT 场景下密码仅在认证阶段使用，认证后放入 SecurityContext 时不需要

**RoseCloud 版 SecurityUser（组合 + UserDetails）：**
```java
public class SecurityUser implements UserDetails {
    private final Long userId;
    private final String username;
    private final String nickname;
    private final String tenantId;
    private final boolean enabled;
    private final UserPrincipal userPrincipal;
    private final String sessionId;
    private final Collection<GrantedAuthority> authorities;

    // --- UserDetails ---
    @Override public Collection<GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return null; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }

    // --- rosecloud 特有 ---
    public Long getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public String getTenantId() { return tenantId; }
    public UserPrincipal getUserPrincipal() { return userPrincipal; }
    public String getSessionId() { return sessionId; }
}
```

### 2.2 UserPrincipal

直接借鉴 thingsboard：
```java
public class UserPrincipal implements Serializable {
    private final Type type;   // USER_NAME | PUBLIC_ID
    private final String value;
}
```
`RestAuthenticationProvider` 根据 `type` 决定认证方式。本期只需 `USER_NAME`，`PUBLIC_ID` 为 OAuth2 / 临时令牌预留。

### 2.3 AbstractJwtAuthenticationToken

直接借鉴 thingsboard：两个构造器（未认证持 raw token / 已认证持 SecurityUser + 擦除 credentials），`setAuthenticated(false)` 只允许从 false 设，不允许从 true 回退。两个子类：

- `JwtAuthenticationToken` — Bearer 请求
- `RefreshAuthenticationToken` — refresh 请求

### 2.4 RawAccessJwtToken / AccessJwtToken / JwtToken

直接借鉴：
```java
public interface JwtToken { String token(); }
public record RawAccessJwtToken(String token) implements JwtToken {}
public record AccessJwtToken(String token) implements JwtToken {}
```

`RawAccessJwtToken` 仅携带原始 string，不包含任何 claims 解析能力。

### 2.5 JwtPair

直接借鉴，去掉 `scope`（本期无 MFA）：
```java
public class JwtPair {
    private String token;          // access token
    private String refreshToken;   // refresh token
}
```

### 2.6 JWT Claims 设计 —— 关键差异

**Thingsboard（丰富）：**
```
subject: email
userId, firstName, lastName, enabled, isPublic, tenantId,
customerId, sessionId, scopes[], exp
```

**RoseCloud（最小化 —— PRD §4.12）：**
```
subject: username
jti:        UUID（会话标识，用于吊销/在线管理）
exp:        过期时间
iat:        签发时间
```

**为什么最小化：**
- PRD §4.12：「JWT 只保存 `username`，租户、角色、权限、用户类型等上下文由服务端根据 `username` 动态补全」
- JWT 长度极短，减轻 header 负担
- 权限变更后下一次请求即生效，无需额外失效机制
- jti 用于会话管理：在线查询、强制下线、登出吊销

**与 thingsboard 的 JwtAuthenticationProvider 差异：**
- Thingsboard：JWT claims 完整反序列化为 `SecurityUser`
- RoseCloud：JWT 仅提取 `username` + `jti` → 调 `UserDetailsService` 动态补全 → 构造 `SecurityUser`
- 模式更像 thingsboard 的 `RefreshTokenAuthenticationProvider`（后者也是 token → 查 user → 重建 SecurityUser）

## 3. 租户登录解析 —— RoseCloud 独有需求

**PRD §4.6：**「用户名/邮箱在租户内唯一（非全局唯一），各租户可各自存在同名用户；普通用户登录需携带租户标识（租户编码），平台管理员登录无需租户标识」

Thingsboard 无此概念。

**LoginRequest：**
```java
public class LoginRequest {
    private String username;      // 登录名（邮箱/手机号）
    private String password;
    private String tenantCode;    // 可选：普通用户需携带，平台管理员不需要
}
```

**RestAuthenticationProvider 登录路由：**
1. `tenantCode` 为空 → 平台管理员登录 → `UserDetailsService.loadUserByUsername(username)`
2. `tenantCode` 不为空 → 租户用户登录 → 需要 username + tenantCode 确定唯一用户

**接口扩展（可选）：**
```java
public interface TenantAwareUserDetailsService extends UserDetailsService {
    UserDetails loadUserByUsername(String username, String tenantCode)
        throws UsernameNotFoundException;
}
```
当 consumer 的 `UserDetailsService` 实现了 `TenantAwareUserDetailsService` 且 `tenantCode` 不为空时，优先用该方法；否则回退到 `loadUserByUsername(tenantCode + "@@" + username)` 约定。

## 4. 租户上下文全链路透传

**TR §6.1：**「租户上下文需在登录解析、网关到服务、服务间调用全链路透传；身份与租户头由网关注入，客户端伪造需被剥离」

Thingsboard 从 JWT claims 取 tenantId，不涉及 header 透传。

RoseCloud 设计：
```
Gateway → 注入 X-Tenant-Id header
       → 剥离客户端自行发送的 X-Tenant-Id

Service → TenantContextFilter 提取并校验 header
        → 设置 TenantContextHolder
```

**TenantContextHolder**（参考 Spring Security `SecurityContextHolder` 模式）：
```java
public final class TenantContextHolder {
    private static final ThreadLocal<String> tenantIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<TenantStatus> tenantStatusHolder = new ThreadLocal<>();
    public static void set(String tenantId, TenantStatus status) { … }
    public static String getTenantId() { … }
    public static void clear() { … }
}
```

**租户状态强制访问控制（TR §7.3）：**
```java
// 在 TenantContextFilter 中
tenant.ACTIVE:   → 正常放行
tenant.EXPIRED:  → 只读（GET/HEAD/OPTIONS 放行，写操作 423）
tenant.FROZEN:   → 阻断登录 + 拒绝所有 API 请求（403）
```

`TenantContextFilter` 根据 `tenantId` 查租户状态并设置 `TenantContextHolder`，filter 层预检。

## 5. 角色体系映射

| RoseCloud 角色（PRD §4.4） | GrantedAuthority | 说明 |
|---------------------------|-----------------|------|
| 平台管理员 | `ROLE_PLATFORM_ADMIN` | 综合运营中心 |
| 租户管理员 | `ROLE_TENANT_ADMIN` | 本租户运营 |
| 普通业务用户 | `ROLE_USER` | 基础工作台 |
| 安全管理员 | `ROLE_SECURITY_ADMIN` | 平台管理员可兼任（PRD §6.1） |

**权限码映射（Menu.perms → GrantedAuthority）**，由 `UserDetailsService` 在构造 authorities 时完成：
- `system:user:delete` → `new SimpleGrantedAuthority("PERM_system:user:delete")`
- 角色编码 → `new SimpleGrantedAuthority("ROLE_TENANT_ADMIN")`
- 用法：`@PreAuthorize("hasAuthority('PERM_system:user:delete')")` 或 `@PreAuthorize("hasRole('TENANT_ADMIN')")`

**注意**：权限码（`PERM_xxx`）由各消费者（如 system 模块）按需在 controller 上添加 `@PreAuthorize` 注解使用。starter 本身不提供 RBAC 服务层，只保证 `SecurityUser.authorities` 正确返回 `ROLE_xxx` + `PERM_xxx` 列表。

## 6. Resource / Operation 枚举（借鉴 thingsboard，本期预留）

虽然本期 RBAC 以 `@PreAuthorize` + `hasRole/hasAuthority` 为主，但借鉴 thingsboard 的 `Resource` + `Operation` 枚举结构，为后续 `PermissionEvaluator` + `@PreAuthorize("hasPermission(...)")` 预留元数据基础。

### 6.1 Resource 枚举

```java
public enum Resource {
    USER,           // 用户管理
    ROLE,           // 角色管理
    MENU,           // 菜单管理
    DEPT,           // 部门管理
    TENANT,         // 租户管理
    TENANT_PROFILE, // 租户套餐
    DICT,           // 字典
    SETTING,        // 参数配置
    NOTICE,         // 通知
    AUDIT_LOG,      // 审计日志
    LOGIN_LOG,      // 登录日志
    SESSION,        // 在线会话
}
```

### 6.2 Operation 枚举

```java
public enum Operation {
    ALL,      // 所有操作
    CREATE,   // 新增
    READ,     // 读取
    WRITE,    // 修改
    DELETE,   // 删除
}
```

### 6.3 后期用法

P1 阶段配合 `PermissionEvaluator`：

```java
@PreAuthorize("hasPermission(#userId, 'USER', 'DELETE')")
public ApiResponse<Void> deleteUser(@PathVariable Long userId) { … }
```

本期只需枚举定义到位，`PermissionEvaluator` 和 `AccessControlService` 不在本期实现范围。枚举的另一个作用是作为 API 文档 / Swagger 的标签元数据，使接口分组更清晰。

## 7. 认证链路

### 7.1 用户名/密码登录

```
POST /api/auth/login  (JSON: {username, password, tenantCode?})
  → RestLoginProcessingFilter.attemptAuthentication()
  → 解析 JSON → LoginRequest
  → UserPrincipal(Type.USER_NAME, username)
  → UsernamePasswordAuthenticationToken(principal, password)
  → RestAuthenticationProvider.authenticate()
  → 路由：tenantCode 为空 → loadUserByUsername(username)
         不为空 → loadUserByUsername(username, tenantCode)
  → PasswordEncoder.matches()
  → SecurityUser(user, enabled, principal)
  → 触发 Consumer<LoginSucceededEvent>
  → RestAwareAuthenticationSuccessHandler
  → tokenFactory.createTokenPair(securityUser) → JwtPair JSON
  → LoginSessionStore.register(jti, securityUser, ip, userAgent)
```

### 7.2 JWT Bearer 请求

```
Authorization: Bearer <token>
  → JwtTokenAuthenticationProcessingFilter.attemptAuthentication()
  → BearerTokenExtractor.extract() → raw token
  → 剥离 X-Tenant-Id / X-User-Id header（如有，仅网关注入值可信）
  → RawAccessJwtToken(raw)
  → JwtAuthenticationToken(unsafeToken)
  → JwtAuthenticationProvider.authenticate()
  → tokenFactory.parseAccessJwtToken(token) → 仅提取 username + jti + exp
  → UserDetailsService.loadUserByUsername(username) → 动态补全
  → TokenOutdatingService 检查 jti 是否被吊销
  → JwtAuthenticationToken(securityUser)
  → SecurityContextHolder.setContext()
  → TenantContextHolder.set(securityUser.tenantId, securityUser.tenantStatus)
```

### 7.3 Refresh Token

```
POST /api/auth/refresh  (JSON: {refreshToken})
  → RefreshTokenProcessingFilter.attemptAuthentication()
  → 检查 HttpMethod.POST
  → RefreshTokenRequest → RawAccessJwtToken
  → RefreshAuthenticationToken(unsafeToken)
  → RefreshTokenAuthenticationProvider.authenticate()
  → tokenFactory.parseRefreshToken() → 提取 username + jti
  → UserDetailsService.loadUserByUsername(username) → 动态补全
  → TokenOutdatingService.isRevoked(jti) 检查
  → 旧 refresh token 的 jti 加入吊销黑名单（防重放）
  → 签发新 JwtPair（新 jti）
```

## 8. 会话管理（P0）

**PRD §4.12：**「在线会话查询、强制下线、登出按 jti 吊销令牌」

```java
public interface LoginSessionStore {
    void register(String jti, SecurityUser user, Instant loginTime,
                  String ip, String userAgent);
    void remove(String jti);
    int removeAllByUser(Long userId);
    List<LoginSession> findByUser(Long userId);
    List<LoginSession> findByTenant(String tenantId);
    long countByTenant(String tenantId);
}

record LoginSession(String jti, Long userId, String username,
                    String nickname, String tenantId,
                    String ip, String userAgent, Instant loginTime) {}
```

P0：`InMemoryLoginSessionStore`（`ConcurrentHashMap<String, LoginSession>` + 按 userId/tenantId 辅助索引），P1 切 Redis。

### TokenOutdatingService（jti 吊销）

```java
public interface TokenOutdatingService {
    void revoke(String jti, Instant expiresAt);
    boolean isRevoked(String jti);
}
```

即使 JWT 最小化仍需要：用户主动登出、管理员强制下线时按 jti 吊销当前令牌。P0 内存 `ConcurrentHashMap<String, Instant>` + 定时清理过期项，P1 切 Redis。

## 9. Security 配置

### 9.1 端点分类

| 路径 | 鉴权 |
|------|------|
| `/api/auth/login` | permitAll |
| `/api/auth/refresh` | permitAll |
| `/api/auth/logout` | authenticated |
| `/api/**` | authenticated |
| `/assets/**`, `/static/**`, `/*.js`, `/*.css`, `/*.ico` | permitAll |

### 9.2 Filter 链路顺序

```
AuthExceptionHandler           (最前 — 全局异常转 JSON)
  → TenantContextFilter        (提取/校验 X-Tenant-Id → TenantContextHolder)
  → RestLoginProcessingFilter  (POST /api/auth/login)
  → RefreshTokenProcessingFilter (POST /api/auth/refresh)
  → JwtTokenAuthenticationProcessingFilter (Bearer 头 → JWT 校验)
  → UsernamePasswordAuthenticationFilter
  → LogoutProcessingFilter     (POST /api/auth/logout — jti 吊销 + LoginSessionStore.remove)
```

关键配置：`sessionCreationPolicy(STATELESS)`、`csrf().disable()`、cors 可配置、`HttpSecurityHeadersCustomizer`（借鉴 thingsboard）。

## 10. 安全事件（函数式扩展）

```java
// Consumer 在 success/failure handler 中被调用
@Bean
Consumer<LoginSucceededEvent> loginSucceededHandler(...);
@Bean
Consumer<LoginFailedEvent> loginFailedHandler(...);
```

在 `RestAwareAuthenticationSuccessHandler` / `RestAwareAuthenticationFailureHandler` 中通过 `List<Consumer>` 注入支持多回调。由 system 模块注册这些 bean 来记录登录日志（审计 IP + User-Agent）。

`RestAuthenticationDetailsSource`（借鉴 thingsboard 同名类）从 `HttpServletRequest` 提取 IP + User-Agent，存入 `LoginSession` 和 `LoginSucceededEvent` / `LoginFailedEvent`。

## 11. 扩展点预留

| 扩展项 | P0 预留方式 |
|--------|-----------|
| OAuth2 | `UserPrincipal.Type.PUBLIC_ID`；`SecurityConfiguration` 可注入 `oauth2Login()` |
| MFA | success handler 预留 MFA token pair 创建点 |
| 临时登录令牌 | `UserPrincipal.Type.PUBLIC_ID` |
| Token/会话存储 Redis | `TokenOutdatingService` + `LoginSessionStore` 接口，P0 内存，P1 切 Redis |
| 租户状态控制 | `TenantContextFilter` 根据 tenantId 查租户状态 → `TenantContextHolder` |
| Method Security（Resource/Operation） | `Resource` + `Operation` 枚举已定义，P1 实现 `PermissionEvaluator` |

## 12. 文件清单（~42 文件）

### model（4 文件）
```
model/SecurityUser.java
model/UserPrincipal.java
model/LoginSession.java
model/TenantStatus.java
```

### rbac（2 文件，本期仅枚举定义）
```
rbac/Resource.java
rbac/Operation.java
```

### token（6 文件）
```
token/JwtToken.java
token/RawAccessJwtToken.java
token/AccessJwtToken.java
token/JwtPair.java
token/JwtTokenFactory.java
token/TokenOutdatingService.java
```

### auth（17 文件）
```
auth/AbstractJwtAuthenticationToken.java
auth/JwtAuthenticationToken.java
auth/RefreshAuthenticationToken.java
auth/TokenExtractor.java
auth/BearerTokenExtractor.java
auth/AuthExceptionHandler.java
auth/rest/LoginRequest.java
auth/rest/RestLoginProcessingFilter.java
auth/rest/RestAuthenticationProvider.java
auth/rest/RestAwareAuthenticationSuccessHandler.java
auth/rest/RestAwareAuthenticationFailureHandler.java
auth/rest/RestAuthenticationDetailsSource.java
auth/jwt/JwtTokenAuthenticationProcessingFilter.java
auth/jwt/JwtAuthenticationProvider.java
auth/jwt/RefreshTokenProcessingFilter.java
auth/jwt/RefreshTokenAuthenticationProvider.java
auth/jwt/SkipPathRequestMatcher.java
```

### context（3 文件）
```
context/TenantContextFilter.java
context/TenantContextHolder.java
context/LogoutProcessingFilter.java
```

### session（3 文件）
```
session/LoginSessionStore.java
session/InMemoryLoginSessionStore.java
session/InMemoryTokenOutdatingService.java
```

### config（3 文件）
```
config/SecurityProperties.java
config/SecurityConfiguration.java
config/HttpSecurityHeadersCustomizer.java
```

### event（2 文件）
```
event/LoginSucceededEvent.java
event/LoginFailedEvent.java
```

### 自动装配（2 文件）
```
RoseCloudSecurityAutoConfiguration.java
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## 13. pom.xml 依赖

```xml
<dependencies>
    <dependency>
        <groupId>io.rosecloud</groupId>
        <artifactId>rosecloud-common-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-autoconfigure</artifactId>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

## 14. 与 thingsboard 关键差异决策

| 决策项 | Thingsboard 做法 | RoseCloud 做法 | 理由 |
|--------|-----------------|---------------|------|
| SecurityUser | `extends User` | 组合 `User`，实现 `UserDetails` | rosecloud `User` 是 `final` |
| 用户查找 | `UserService.findUserByEmail()` | `UserDetailsService.loadUserByUsername()` | Spring Security 标准生态 |
| JWT claims | 丰富（firstName, tenantId…） | 最小化（username + jti + exp） | PRD §4.12：上下文服务端动态补全 |
| 角色来源 | `Authority` 枚举 | `UserDetailsService.authorities` 动态获取 | 支持自定义角色 |
| RBAC P0 | Resource/Operation → PermissionChecker → AccessControlService | `@PreAuthorize` + `hasRole/hasAuthority` | 贴合 Menu.perms 设计；Resource/Operation 枚举预留 |
| 租户登录 | 无概念 | 用户需租户标识，管理员无需 | PRD §4.6 |
| 租户上下文 | JWT claims → tenantId | Gateway header 注入 → 全链路透传 | TR §6.1 |
| 会话管理 | 无 | P0 在线查询 + 强制下线 + jti 吊销 | PRD §4.12 |
| Token 过期 | TokenOutdatingService（Cache） | jti 吊销黑名单（P0 内存） | JWT 最小化 + 每次请求重载 |
| 错误响应 | ThingsboardErrorResponseHandler | `ApiResponse` + `@ControllerAdvice` | 复用 rosecloud 现有体系 |
| UA 解析 | `ua_parser` 库 | String userAgent（通过 RestAuthenticationDetailsSource 提取） | 减少依赖 |

## 15. 实施建议

1. 建 `pom.xml` + `AutoConfiguration.imports`，注册到 `rosecloud-starter-tech`
2. 实现 model：`SecurityUser`、`UserPrincipal`、`TenantStatus`、`LoginSession`
3. 实现 enums：`Resource`、`Operation`
4. 实现 record：`RawAccessJwtToken`、`AccessJwtToken`、`JwtToken`
5. 实现 token 层：`JwtTokenFactory`（最小化 claims）、`JwtPair`、`TokenOutdatingService`（接口）
6. 实现会话管理：`LoginSessionStore`（接口）+ `InMemoryLoginSessionStore`、`InMemoryTokenOutdatingService`
7. 实现认证层：`AbstractJwtAuthenticationToken` → 两个子类、`BearerTokenExtractor`、`RestLoginProcessingFilter` + `RestAuthenticationProvider`、`JwtTokenAuthenticationProcessingFilter` + `JwtAuthenticationProvider`、`RefreshTokenProcessingFilter` + `RefreshTokenAuthenticationProvider`、`RestAwareAuthenticationSuccessHandler` / `FailureHandler`、`RestAuthenticationDetailsSource`
8. 实现上下文基础设施：`TenantContextHolder`、`TenantContextFilter`、`LogoutProcessingFilter`、`SkipPathRequestMatcher`、`AuthExceptionHandler`
9. 实现 `SecurityConfiguration` + `HttpSecurityHeadersCustomizer`
10. 实现 `SecurityProperties`
11. 重构 auth 模块为新的 starter 消费者（大幅简化或移除）
12. 重构 system 模块：实现 `UserDetailsService`（实现 `TenantAwareUserDetailsService`）
13. 移除旧 starter 代码，验证所有消费者（auth/system/notice/monolith）可正常启动
