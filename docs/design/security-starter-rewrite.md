# rosecloud-starter-security 重构设计

> 参考 ThingsBoard 从零重写，以 UserDetailsService 为唯一必需接入点。

---

## 1. 设计目标

| 维度 | 目标 |
|------|------|
| 高内聚 | SecurityUser 承载所有认证态字段 + UserDetails 实现 |
| 低耦合 | 唯一强依赖 UserDetailsService；其余均可 @ConditionalOnMissingBean 覆盖 |
| 接口注入 | 可扩展点均为接口或 @FunctionalInterface |
| 函数式扩展 | Consumer\<LoginSucceededEvent\> / Consumer\<LoginFailedEvent\> |
| 内建安全 | JWT 最小 claims、jti 吊销、会话管理、CSRF/headers 默认 |
| RBAC | Resource/Operation/PermissionChecker/AccessControlService |

---

## 2. 核心模型

### 2.1 SecurityUser（参照 ThingsBoard）

不从系统 User 继承（starter 不依赖消费者模块），字段名与语义对齐 ThingsBoard。

```java
public class SecurityUser implements UserDetails {
    private Long userId;
    private String email;
    private Authority authority;
    private String tenantId;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private UserPrincipal userPrincipal;
    private String sessionId;
    private Collection<GrantedAuthority> authorities;  // lazy from authority
}
```

### 2.2 UserPrincipal

```java
public class UserPrincipal {
    public enum Type { USER_NAME, PUBLIC_ID }
    private final Type type;
    private final String value;
}
```

### 2.3 Authority

```java
public enum Authority {
    SYS_ADMIN(0), TENANT_ADMIN(1), BUSINESS_USER(2),
    REFRESH_TOKEN(10), PRE_VERIFICATION_TOKEN(11)
}
```

---

## 3. 认证流程

### 3.1 登录

```
POST /api/auth/login
  → RestLoginProcessingFilter
    → RestAuthenticationProvider (PasswordEncoder + UserDetailsService)
    → RestAwareAuthenticationSuccessHandler
      → JwtTokenFactory.createTokenPair(securityUser)
      → LoginStore.register(jti, ...)
      → publish LoginSucceededEvent
      → 200 { token, refreshToken }
```

### 3.2 JWT 鉴权

```
Request → JwtTokenAuthenticationProcessingFilter
  → BearerTokenExtractor
  → JwtAuthenticationProvider
    → JwtTokenFactory.parseAccessJwtToken → claims
    → jti 吊销检查
    → UserDetailsService.loadUserByUsername (动态补全)
    → SecurityContextHolder
```

### 3.3 Refresh

```
POST /api/auth/refresh { refreshToken }
  → RefreshTokenProcessingFilter
  → RefreshTokenAuthenticationProvider
    → parseRefreshToken → jti check → loadUser → createTokenPair
```

---

## 4. JWT Claims

```
sub: email
jti: UUID
userId: Long
authority: "SYS_ADMIN"
tenantId: String
firstName: String
lastName: String
enabled: boolean
sessionId: UUID
iat, exp
```

access token 无 type；refresh token 含 `type: refresh`。

---

## 5. RBAC

```java
@PreAuthorize("@acs.checkPermission(#user, 'SYSTEM_USER', 'READ')")
```

- Resource: SYSTEM_USER, TENANT, ROLE, MENU, SETTING, AUDIT_LOG, LOGIN_SESSION
- Operation: ALL, CREATE, READ, WRITE, DELETE
- PermissionChecker: 函数式，可按 Resource + Authority 组合
- AccessControlService: checkPermission / hasPermission

---

## 6. 事件

| 事件 | 用途 |
|------|------|
| LoginSucceededEvent | 记录登录日志 |
| LoginFailedEvent | 记录失败日志 |

消费者以 Consumer\<T\> bean 注入。

---

## 7. 内建安全

- CSRF disable（API 服务）
- CORS 默认宽松（可覆盖）
- Security Headers: cache-control, XSS, frame-options 等
- 请求体 payload size 检查（可选）
- 公共路径白名单配置

---

## 8. 预留扩展点

| 扩展点 | 机制 |
|--------|------|
| OAuth2 | OAuth2AuthorizationRequestResolver + oauth2Login() |
| MFA | MfaAuthenticationToken + TwoFactorAuthService |
| API Key | ApiKeyAuthenticationProvider |
