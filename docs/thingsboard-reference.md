# ThingsBoard 平台能力源码解析与 RoseCloud 借鉴参考

> 源码基准：`~/github/thingsboard`，浅克隆最新 master，提交 `9a4f1c5`。下文 ThingsBoard 文件以 `../thingsboard/...` 引用，行号基于该提交。
>
> 对照基准：RoseCloud 现状见 `rosecloud-starter-tech/`、`rosecloud-starter-business/`、`rosecloud-service/`（auth/gateway/system/notice/monolith）。
>
> 路线图基线（`docs/01-requirements.md` §4.12、§5.2、§5.4、§5.5）：OAuth2 与 MFA 属 P1，**默认关闭但必须预留**；完整 OAuth2 Authorization Server 与全量 MFA 不在第一阶段做满。
>
> 用途：认证体系（JWT/OAuth2/MFA）做"可借鉴分析"；系统/用户/通知等做"功能实现参考清单"。

---

## 目录

- Part A 认证体系：JWT / OAuth2 / MFA
  - A1 JWT 功能列表
  - A2 OAuth2 功能列表
  - A3 MFA 功能列表
  - A4 认证安全设置与策略
  - A5 认证实现解析（源码要点）
  - A6 认证对照与落地建议
- Part B 平台能力功能列表（实现参考）
  - B1 系统管理
  - B2 用户管理
  - B3 通知中心
  - B4 其他可参考能力
  - B5 实体与关系模型
  - B6 告警体系
  - B7 看板与组件
  - B8 任务与事件
  - B9 资源与文件
  - B10 队列、规则引擎与计算字段
  - B11 IoT 专属能力（不适用）
  - B12 集成与扩展
  - B13 租户管理（Tenant/TenantProfile 套餐配额）
  - B14 客户管理（Customer）
  - B15 设备管理（Device/DeviceProfile）
  - B16 资产管理（Asset/AssetProfile）
  - B17 客户与临时令牌登录（新方向借鉴）
- Part C 风险取舍与结论

---

# Part A 认证体系：JWT / OAuth2 / MFA

## A1 JWT 功能列表

| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| 令牌签发 | HS512 对称签名；access 含 `userId/tenantId/customerId/scopes/sessionId/isPublic`，refresh scope=`REFRESH_TOKEN` | `../thingsboard/application/.../model/token/JwtTokenFactory.java` |
| Token Pair | `createTokenPair` 设 `sessionId=UUID` 并写入 access+refresh | 同上 |
| 令牌类型/Scope | `ACCESS`/`REFRESH_TOKEN`/`PRE_VERIFICATION_TOKEN`/`MFA_CONFIGURATION_TOKEN`；`parseAccessJwtToken`(:139) 拒绝 MFA scope 当 access 用 | `../thingsboard/common/data/.../security/Authority.java` |
| Access 校验 | `JwtAuthenticationProvider` + `JwtTokenAuthenticationProcessingFilter`（bearer header） | `../thingsboard/application/.../jwt/` |
| Refresh 校验/刷新 | `RefreshTokenAuthenticationProvider` + `RefreshTokenProcessingFilter`（`/api/auth/token`） | 同上 |
| JWT 设置管理 | `JwtSettingsService` 存 `AdminSettings`(key=`"jwt"`)：`getJwtSettings`/`reloadJwtSettings`/`saveJwtSettings` | `.../jwt/settings/DefaultJwtSettingsService.java`(:49,:65) |
| 密钥热轮换 | `saveJwtSettings`→`reloadJwtSettings()`→`JwtTokenFactory.reload()`+集群广播；存库后 YAML/ENV 被忽略(:57) | 同上 |
| 会话治理 | `sessionId` claim + `UserSessionInvalidationEvent`(按 sessionId 失效) + `UserCredentialsInvalidationEvent`(改密失效) | `../thingsboard/common/data/.../security/event/` |
| 登出 | `/api/auth/logout` | `.../controller/AuthController.java`(:97) |

## A2 OAuth2 功能列表

> 本版本仅实现 **OAuth2 客户端登录**（第三方/社交登录），未见 Authorization Server / client_credentials。

| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| 第三方登录装配 | `http.oauth2Login(...)`，登录页 `/oauth2Login`，成功/失败 handler | `.../config/ThingsboardSecurityConfiguration.java`(:282) |
| DB 驱动动态注册 | `HybridClientRegistrationRepository`：registrationId=UUID 从库查 `OAuth2Client`→构造 Spring `ClientRegistration`(AUTHORIZATION_CODE) | `../thingsboard/dao/.../oauth2/HybridClientRegistrationRepository.java` |
| 客户端管理 API | 保存/查 infos/按 ids 查/按 id 查/删除/登录处理 URL | `.../controller/OAuth2Controller.java` |
| 客户端实体 | `OAuth2Client`(DB)：tenantId/title/mapperConfig/clientId/secret/各 endpoint/scope/loginButtonLabel/Icon/platforms/providerName | `../thingsboard/common/data/.../oauth2/OAuth2Client.java` |
| 配置模板管理 | OAuth2ClientRegistrationTemplate 的保存/删除/列出 | `.../controller/OAuth2ConfigTemplateController.java` |
| 域名管理 | domain 创建/绑定 oauth2Clients/查 infos/删除——多域名挂不同提供商 | `.../controller/DomainController.java` |
| 用户映射 SPI | `OAuth2ClientMapper`(BASIC/CUSTOM/GITHUB/APPLE) + `OAuth2ClientMapperProvider` 路由 | `.../oauth2/OAuth2ClientMapperProvider.java`(:46) |
| BASIC 映射 | emailAttributeKey/姓名 attribute/tenantNameStrategy(DOMAIN/EMAIL/CUSTOM)/tenantNamePattern/customerNamePattern | `.../oauth2/BasicOAuth2ClientMapper.java`、`OAuth2BasicMapperConfig.java` |
| CUSTOM 映射 | 调外部 REST 端点(url/username/password/sendToken) | `OAuth2CustomMapperConfig.java`、`CustomOAuth2ClientMapper.java` |
| 自动建用户 | `allowUserCreation`/`activateUser`，`AbstractOAuth2ClientMapper.getOrCreateUser`(`userCreationLock` 防并发) | `.../oauth2/AbstractOAuth2ClientMapper.java`(:145) |
| 无状态授权请求 | `HttpCookieOAuth2AuthorizationRequestRepository`：`oauth2_auth_request`+`prev_uri` cookie(180s) | `.../oauth2/HttpCookieOAuth2AuthorizationRequestRepository.java`(:31) |
| 成功回跳 | 映射用户→`createTokenPair`→重定向带 `accessToken`/`refreshToken` 参数 | `.../oauth2/Oauth2AuthenticationSuccessHandler.java`(:110,:133) |
| 登录页元数据 | `/api/noauth/oauth2Clients` 按域名返回可用提供商 | `.../controller/OAuth2Controller.java`(:80) |
| SMTP OAuth2 | 邮件发送方 OAuth2 登录(`/api/admin/mail/oauth2/*`)，用于邮件服务器授权 | `.../controller/AdminController.java`(:410,:419) |
| 注意 | 本版本 OAuth2 登录**不触发 MFA**（成功处理器直接签正式 pair） | `Oauth2AuthenticationSuccessHandler.java`(:110) |

## A3 MFA 功能列表

| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| Provider 类型 | TOTP / SMS / EMAIL / BACKUP_CODE | `.../mfa/provider/TwoFaProviderType.java` |
| Provider SPI | `TwoFaProvider<C,A>`：generateNewAccountConfig/prepareVerificationCode/checkVerificationCode/check/getType | `.../mfa/provider/TwoFaProvider.java` |
| TOTP | aerogear-otp，Base32 20 字节密钥 + `otpauth://totp` URL(QR)，`new Totp(secret).verify(code)` | `.../provider/impl/TotpTwoFaProvider.java`(:48,:64) |
| SMS/EMAIL OTP | `OtpBasedTwoFaProvider`：6 位数字，存 `TWO_FA_VERIFICATION_CODES_CACHE`(含 lifetime) | `.../provider/impl/OtpBasedTwoFaProvider.java`(:42,:52) |
| BACKUP_CODE | N 个 8 位 hex，命中即移除并持久化(消耗式) | `.../provider/impl/BackupCodeTwoFaProvider.java` |
| 平台设置(系统级) | `PlatformTwoFaSettings`：providers/minVerificationCodeSendPeriod/verificationCodeCheckRateLimit/maxVerificationFailuresBeforeUserLockout/totalAllowedTimeForVerification/enforceTwoFa/enforcedUsersFilter | `.../security/model/mfa/PlatformTwoFaSettings.java` |
| 账户设置(用户级) | `AccountTwoFaSettings`：`LinkedHashMap<providerType, config>` | `.../security/model/mfa/account/AccountTwoFaSettings.java` |
| 账户配置 API | get/generate/submit/verifyAndSave/update/delete account config | `.../controller/TwoFactorAuthConfigController.java` |
| 平台配置 API | get/save `PlatformTwoFaSettings`、get available provider types | 同上(:213,:260) |
| 登录验证 API | `/verification/send`、`/verification/check`、`/providers`、`/login`(pre-auth 换正式 token) | `.../controller/TwoFactorAuthController.java`(:74,:87,:110,:141) |
| pre-auth 令牌流程 | 密码通过→`isTwoFaEnabled`→`MfaAuthenticationToken`；`isEnforceTwoFaEnabled`→`MfaConfigurationToken` | `.../rest/RestAuthenticationProvider.java`(:28) |
| pre-auth 签发 | `RestAwareAuthenticationSuccessHandler` 签 `PRE_VERIFICATION_TOKEN`(TTL=`totalAllowedTimeForVerification`，默认 30min，refreshToken=null) | `.../rest/RestAwareAuthenticationSuccessHandler.java`(:54,:69) |
| 强制 2FA | `enforceTwoFa`+`enforcedUsersFilter`→`MFA_CONFIGURATION_TOKEN`(强制先配置) | 同上 |
| 限频与锁定 | `RateLimitService`(SEND/CHECK)+`maxVerificationFailuresBeforeUserLockout`→`validateTwoFaVerification` | `.../mfa/DefaultTwoFactorAuthService.java`(:95,:135,:151) |
| 配置管理器 | `TwoFaConfigManager`：账户/平台 get/save/delete，租户级覆盖回退系统级 | `.../mfa/config/TwoFaConfigManager.java` |

## A4 认证安全设置与策略

| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| SecuritySettings | passwordPolicy/maxFailedLoginAttempts/userLockoutNotificationEmail/mobileSecretKeyLength/userActivationTokenTtl(1-24h)/passwordResetTokenTtl(1-24h) | `.../security/model/SecuritySettings.java` |
| UserPasswordPolicy | min/max length、大小写/数字/特殊字符最少数、allowWhitespaces、forceUserToResetPasswordIfNotValid、passwordExpirationPeriodDays、passwordReuseFrequencyDays | `.../security/model/UserPasswordPolicy.java` |
| 安全设置 API | `/api/admin/securitySettings` get/save | `.../controller/AdminController.java`(:165,:174) |
| JWT 设置 API | `/api/admin/jwtSettings` get/save（热轮换） | 同上(:186,:195) |
| 通用 AdminSettings | `/api/admin/settings/{key}` get、`/api/admin/settings` save（mail/sms 等配置） | 同上(:127,:145) |
| 邮件/短信测试 | `/api/admin/settings/testMail`、`/api/admin/settings/testSms` | 同上(:209,:247) |
| 功能开关暴露 | `FeaturesInfo`：emailEnabled/smsEnabled/notificationEnabled/oauthEnabled/twoFaEnabled | `../thingsboard/.../FeaturesInfo.java`、`/api/admin/featuresInfo` |
| API Key 认证 | 每用户 API key：save/list/updateDesc/enable/disable/delete；`ApiKeyTokenAuthenticationProcessingFilter` 作为 JWT 之外认证方式 | `.../controller/ApiKeyController.java` |
| 用户激活/重置 | checkActivateToken/requestResetPasswordByEmail/checkResetToken/activate/resetPassword/changePassword/getUserPasswordPolicy | `.../controller/AuthController.java`(:86-242) |

## A5 认证实现解析（源码要点）

### A5.1 JWT
`JwtTokenFactory`（`.../model/token/JwtTokenFactory.java`）用 HS512，claims 含 `sessionId`；`createTokenPair` 设 sessionId 写入 access+refresh；`createMfaToken`( :182) 签单 scope 令牌；`parseAccessJwtToken`( :139) 拒绝 MFA scope 当 access。`DefaultJwtSettingsService`( :49) 把 `JwtSettings` 存 `AdminSettings`(key `jwt`)，`saveJwtSettings`→`reloadJwtSettings()`→`JwtTokenFactory.reload()`( :68) 实现热轮换，存库后 YAML/ENV 忽略( :57)。会话治理经 `sessionId`+`UserSessionInvalidationEvent`/`UserCredentialsInvalidationEvent`。

### A5.2 MFA
`DefaultTwoFactorAuthService`(`.../mfa/`) 用 `EnumMap<TwoFaProviderType, TwoFaProvider>` 收集 provider( :57,:188)，`RateLimitService` 限频( :95,:135)，`validateTwoFaVerification` 锁定( :151)。`TotpTwoFaProvider` 用 aerogear-otp + Base32 密钥 + `otpauth://` URL；`OtpBasedTwoFaProvider` 6 位 OTP 存缓存。登录流程：`RestAuthenticationProvider`( :28) 密码通过后按 `isTwoFaEnabled`/`isEnforceTwoFaEnabled` 返回 `MfaAuthenticationToken`/`MfaConfigurationToken`；`RestAwareAuthenticationSuccessHandler`( :54) 据此签 `PRE_VERIFICATION_TOKEN`/`MFA_CONFIGURATION_TOKEN`（TTL=`totalAllowedTimeForVerification`，默认 30min，refreshToken=null）。`TwoFactorAuthController` 的 `/verification/send`、`/verify` 以 `@PreAuthorize("hasAuthority('PRE_VERIFICATION_TOKEN')")` 守卫。

### A5.3 OAuth2
`HybridClientRegistrationRepository`(`dao/.../oauth2/`) 按 UUID 从库查 `OAuth2Client`→`toSpringClientRegistration` 构造 Spring `ClientRegistration`(AUTHORIZATION_CODE)。`OAuth2Client` 为 DB 实体、按租户归属。`OAuth2ClientMapperProvider`( :46) 按 `MapperType`(BASIC/CUSTOM/GITHUB/APPLE) 路由 mapper；`AbstractOAuth2ClientMapper.getOrCreateUser` 按 email 查用户或自动建 user/tenant/customer(`userCreationLock`，`activateUser` :145)。`HttpCookieOAuth2AuthorizationRequestRepository` 把授权请求存 cookie(180s)。`Oauth2AuthenticationSuccessHandler`( :110) 映射后 `createTokenPair` 并重定向带 token。**本版本 OAuth2 登录不触发 MFA**( :110)。

## A6 认证对照与落地建议

### A6.1 对照（RoseCloud 现状 vs ThingsBoard）
- **JWT**：RoseCloud 已有 HS256 + `jti` 吊销 + `sys_login_session`；可借鉴 `sessionId` 会话治理（会话上限/改密失效）、密码重置/激活专用令牌、JWT 配置入库热轮换（RoseCloud 现 Nacos 共享配置，按需权衡）。
- **MFA**：RoseCloud 暂无；可借鉴 Provider SPI + pre-auth 令牌流程。pre-auth 复用 `JwtTokenCodec`，SMS/Email OTP 复用 `rosecloud-notice` 的 `NoticeChannelSender`，OTP 存 Redis，零新基建。
- **OAuth2**：RoseCloud 仅资源服务器；可借鉴客户端登录 + DB 驱动动态注册 + 可插拔 mapper SPI + Cookie 无状态授权请求 + 成功后签本站 JWT。

### A6.2 落地建议（分阶段，默认关闭）
- **阶段 0（低成本增量）**：会话数上限（复用 `sys_login_session`，超限吊销最早 `jti`）；改密失效旧令牌（`sys_user.password_changed_at`，校验比 `iat`）；密码重置/激活令牌（`TokenType` 扩展，接入 `TenantProvisioner`）。
- **阶段 1（MFA，P1 占位）**：新建 `rosecloud-mfa-starter`（`rosecloud.mfa.enabled` 门控）+ `TwoFactorAuthProvider` SPI；先 TOTP + BackupCode，SMS/Email 调 notice；`TokenType.PRE_AUTH` 短 TTL + `/api/v1/auth/mfa/verify`；限频复用 Redis；存 `sys_user_mfa`。
- **阶段 2（OAuth2 客户端登录，P1）**：新建 `rosecloud-oauth2-client-starter`；`sys_oauth2_client` 表 + `ClientRegistrationRepository` 动态构造；`OAuth2ClientMapper` SPI(BASIC/CUSTOM)；Cookie 仓库；成功处理器签本站 JWT；如需 MFA 在此补 `isTwoFaEnabled` 分支；登录页元数据 API + 网关白名单加回调。
- **阶段 3（暂缓）**：OAuth2 Authorization Server(M2M)、非对称签名+JWKS、`enforceTwoFa`、public 令牌——仅预留边界。

---

# Part B 平台能力功能列表（实现参考）

> 以下为 ThingsBoard 平台能力清单，作为 RoseCloud 对应模块的功能实现参考。RoseCloud 现有：system（用户/租户/任务/会话/登录日志）、notice（站内/邮件/短信）、common-core（ApiResponse/ErrorCode）。借鉴时按 RoseCloud 架构（starter/Feign/多租户）裁剪。

## B1 系统管理

| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| 通用 AdminSettings | key-value 管理设置 get/save（mail/sms/general 等） | `.../controller/AdminController.java`(:127,:145) |
| 安全设置 | SecuritySettings get/save | 同上(:165,:174) |
| JWT 设置 | get/save + 热轮换 | 同上(:186,:195) |
| 邮件/短信测试 | testMail/testSms | 同上(:209,:247) |
| 系统信息 | `/api/admin/systemInfo`、`/api/system/info`、`/api/system/params` | `AdminController.java`(:392)、`SystemInfoController.java`(:117,:124) |
| 功能信息 | `/api/admin/featuresInfo`（email/sms/notification/oauth/twoFa 开关） | `AdminController.java`(:401) |
| 平台更新检查 | `/api/admin/updates` | 同上(:383) |
| 版本控制仓库设置 | repositorySettings：get/exists/info/save/delete/checkAccess | 同上(:265-331) |
| 自动提交设置 | autoCommitSettings：get/exists/save/delete | 同上(:343-372) |
| 域名管理 | domain 创建/绑定 oauth2Clients/查 infos/删除 | `.../controller/DomainController.java` |
| UI 设置 | `/api/uiSettings/helpBaseUrl` | `.../controller/UiSettingsController.java` |
| 使用量信息 | `/api/usage` | `.../controller/UsageInfoController.java` |

> 借鉴点：RoseCloud 可补"平台功能开关暴露 API"（对齐 `FeaturesInfo`），把 OAuth2/MFA/通知等开关集中给前端；系统信息/更新检查按需。

## B2 用户管理

| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| 用户 CRUD | saveUser/getUserById/deleteUser/getUsers(分页)/findUsersByQuery | `.../controller/UserController.java` |
| 按角色查询 | getTenantAdmins、getCustomerUsers、getUsersByIds、getUsers/list | 同上(:338,:361,:610,:625) |
| 用户字段 | tenantId/customerId/email/authority/firstName/lastName/phone/version | `.../common/data/User.java`(:42-56) |
| 凭据 | enabled/password/activateToken(+expTime)/resetToken(+expTime)/lastLoginTs/failedLoginAttempts | `.../security/UserCredentials.java`(:36-44) |
| 启用/停用凭据 | `/user/{id}/userCredentialsEnabled` | `UserController.java`(:386) |
| 激活流程 | sendActivationMail/getActivationLink/getActivationLinkInfo/checkActivateToken/activate | `UserController.java`(:208,:230)、`AuthController.java`(:134,:202) |
| 密码重置 | requestResetPasswordByEmail/checkResetToken/resetPassword | `AuthController.java`(:155,:174,:242) |
| 改密 | `/auth/changePassword` | `AuthController.java`(:105) |
| 密码策略 | `/noauth/userPasswordPolicy` | `AuthController.java`(:128) |
| Token 访问 | isUserTokenAccessEnabled/getUserToken（用户模拟） | `UserController.java`(:155,:160) |
| 用户设置 | JSON 设置 save/get/update/delete，按 type 分组(general+自定义) | 同上(:449-540) |
| 最近看板 | getLastVisitedDashboards/reportUserDashboardAction | 同上(:560,:571) |
| 移动会话 | `/user/mobile/session` get/post/delete | 同上(:587-602) |
| 用户分配 | getUsersForAssign（告警指派） | 同上(:408) |

> 借鉴点：RoseCloud `sys_user` 已有基础字段；可补 `password_changed_at`/`failed_login_attempts`/`last_login_ts` 支撑改密失效与登录锁定；用户激活/重置令牌复用 JWT 专用 type；`UserSettings`(JSON) 可作为用户偏好存储参考。Token 访问（模拟）与移动会话按需。

## B3 通知中心

| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| 通知查询 | getNotifications(分页+unreadOnly)、getUnreadNotificationsCount | `.../controller/NotificationController.java`(:173,:198) |
| 通知状态 | markNotificationAsRead、markAllNotificationsAsRead、deleteNotification | 同上(:209,:221,:233) |
| 通知请求 | createNotificationRequest、getPreview、getById、getRequests、delete | 同上(:271,:329,:435,:446,:469) |
| 配额提升请求 | sendEntitiesLimitIncreaseRequest（通知系统管理员） | 同上(:299) |
| 通知目标 | save/get/getByIds/list/byNotificationType/delete、getRecipientsForNotificationTargetConfig(预览接收人) | `.../controller/NotificationTargetController.java` |
| 目标类型 | PLATFORM_USERS(web/email/sms/mobile)、SLACK、MICROSOFT_TEAMS | `.../notification/targets/NotificationTargetType.java` |
| 平台用户过滤器 | USER_LIST/CUSTOMER_USERS/TENANT_ADMINISTRATORS/AFFECTED_TENANT_ADMINISTRATORS/SYSTEM_ADMINISTRATORS/ALL_USERS/ORIGINATOR_ENTITY_OWNER_USERS/AFFECTED_USER | `.../targets/platform/UsersFilterType.java` |
| 通知模板 | save/get/list/delete；`NotificationTemplateConfig`(按 deliveryMethod→模板) | `.../controller/NotificationTemplateController.java` |
| 投递方式 | WEB/EMAIL/SMS/SLACK/MICROSOFT_TEAMS/MOBILE_APP | `.../notification/NotificationDeliveryMethod.java` |
| 通知规则 | save/get/list/delete | `.../controller/NotificationRuleController.java` |
| 规则触发类型 | ENTITY_ACTION/ALARM/ALARM_COMMENT/ALARM_ASSIGNMENT/DEVICE_ACTIVITY/RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT/EDGE_CONNECTION/EDGE_COMMUNICATION_FAILURE/NEW_PLATFORM_VERSION/ENTITIES_LIMIT/API_USAGE_LIMIT/RATE_LIMITS/TASK_PROCESSING_FAILURE/RESOURCES_SHORTAGE（部分仅系统级） | `.../rule/trigger/config/NotificationRuleTriggerType.java` |
| 规则接收人 | DefaultNotificationRuleRecipientsConfig、EscalatedNotificationRuleRecipientsConfig(升级) | `.../rule/` |
| 通知设置 | save/get NotificationSettings(投递方式配置)、getAvailableDeliveryMethods | `NotificationController.java`(:493,:506,:517) |
| 用户偏好 | UserNotificationSettings：按 NotificationType × DeliveryMethod 开关 | `.../settings/UserNotificationSettings.java` |
| 请求/通知状态 | RequestStatus: PROCESSING/SENT/SCHEDULED；NotificationStatus: SENT/READ | `.../notification/NotificationRequestStatus.java`、`NotificationStatus.java` |
| 请求配置 | `sendingDelayInSec`（定时发送，≤1 周） | `.../notification/NotificationRequestConfig.java` |

> 借鉴点：RoseCloud `rosecloud-notice` 已有站内(拉取)/邮件/短信 + `NoticeChannelSender` SPI + 位掩码 channels + `NoticeRecipientApi` 解析接收人。ThingsBoard 的**通知规则（触发器→目标→模板）自动化**与**用户级偏好开关**是 RoseCloud v1 之后的增强方向；目标"平台用户过滤器"（按角色/租户/受影响用户解析）可对齐 RoseCloud 的全局/租户/角色解析。

## B4 其他可参考能力

| 能力 | 说明 | ThingsBoard 位置 |
|---|---|---|
| 审计日志 | 实体操作审计（登录/锁定/CRUD 等），含 client/browser/os/device | `.../controller/AuditLogController.java`、`DefaultSystemSecurityService.logLoginAction` |
| 租户/租户配置 | Tenant CRUD、TenantProfile（配额/功能开关） | `TenantController.java`、`TenantProfileController.java` |
| API Key 认证 | 每用户 API key，作为 JWT 之外的访问方式 | `ApiKeyController.java` |
| 资源/OTA/队列/规则链 | IoT 特有能力 | `TbResourceController` 等（RoseCloud 按需，多数不适用） |

> 借鉴点：RoseCloud 已有审计 starter（`@AuditLog` + AOP）与租户/套餐占位；ThingsBoard 的"登录审计含 UA 解析"与"API Key 认证"可作为后续安全增强参考。

---

## B5 实体与关系模型（统一实体体系）

> ThingsBoard 统一实体模型：核心对象（Tenant/Customer/User/Device/Asset/Dashboard/EntityView/RuleChain/Edge 等）均继承 `BaseData<UUID>`，实现 `HasName`/`HasTenantId`/`HasCustomerId`；实体间经 `EntityRelation`(from/to/type/typeGroup) 建立关系，支持关系树遍历。

| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| 实体关系 | 创建/删除关系、按 from/to/type 查询、按 query 遍历关联实体与 infos | `.../controller/EntityRelationController.java` |
| EntityView | 实体视图（过滤遥测/属性），分配 customer/公开、按 query 查、按类型查 | `.../controller/EntityViewController.java` |
| 实体查询 | 按 query 计数/查询实体数据、查询/计数告警（关系遍历） | `.../controller/EntityQueryController.java` |
| 通用模式 | 实体 CRUD + 分页 infos + 按 ids 批量 + query 查询 + assign/unassign to customer/public/edge | 各实体 Controller |

> 借鉴价值：RoseCloud 可借鉴"统一实体 + 关系图"做组织/部门/资源层级与权限继承；EntityView 的"数据范围视图"对齐数据权限；EntityQuery 的关系遍历查询可复用。

## B6 告警体系

| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| Alarm 生命周期 | 创建/更新/删除、ack（确认）、clear（清除）、assign/unassign（指派） | `.../controller/AlarmController.java`(:127,:162,:177,:192) |
| Alarm 查询 | 按实体/全部查询(v1/v2)、最高严重度、告警类型 | 同上(:230,:282,:457,:488) |
| AlarmRule | 规则告警 CRUD、按实体查、按名称查、调试事件、TBEL 表达式测试 | `.../controller/AlarmRuleController.java` |
| AlarmComment | 告警评论创建/更新/删除/查询 | `.../controller/AlarmCommentController.java` |

> 借鉴价值：通用告警/事件闭环（创建→确认→清除→指派）+ 规则驱动 + 评论协作，可作 RoseCloud 运维告警或业务事件通知骨架。

## B7 看板与组件

| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| Dashboard | CRUD、分配/取消分配 customer、批量更新 customers、公开/取消公开、租户/客户分页、Home Dashboard | `.../controller/DashboardController.java` |
| Dashboard 工具 | getServerTime、getMaxDatapointsLimit、按 ids 查 | 同上(:117,:139,:624) |
| WidgetType | 组件类型 CRUD、按 bundle 查、FQN | `.../controller/WidgetTypeController.java` |
| WidgetsBundle | 组件包 CRUD、更新包内组件、分页、按 ids 查 | `.../controller/WidgetsBundleController.java` |

> 借鉴价值：RoseCloud 工作台/可视化可借鉴"看板 + 组件包 + 组件类型"分层与分配/公开模型。

## B8 任务与事件

| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| Job | 异步作业查询/列表/取消/重处理/删除 | `.../controller/JobController.java`(:58,:65,:93,:101,:109) |
| Event | 按类型/过滤器查事件、清除事件（debug/lifecycle） | `.../controller/EventController.java`(:117,:194,:235) |

> 借鉴价值：如果后续 RoseCloud 再引入独立异步编排能力，可借鉴 Job 的 cancel/reprocess API 与 Event 的按 filter 查询/清除；当前版本不保留独立任务中心。

## B9 资源与文件

| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| TbResource | 资源上传(multipart)/下载、按类型下载(LwM2M/PKCS12/JKS/JS)、infos 分页、按 ids 查、ETag 缓存 | `.../controller/TbResourceController.java` |
| Image | 图片上传/更新/信息、公开资源(publicResourceKey)、导出/导入、预览、分页、删除 | `.../controller/ImageController.java` |

> 借鉴价值：RoseCloud 文件/资源模块可借鉴"上传 + 公私 + ETag 缓存 + 导入导出"；Image 的 publicResourceKey 适合公开资源分发。

## B10 队列、规则引擎与计算字段

| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| Queue | 队列管理（按 serviceType 查、按 id/name 查、CRUD，系统级） | `.../controller/QueueController.java` |
| QueueStats | 队列统计查询（分页/按 id/批量/列表） | `.../controller/QueueStatsController.java` |
| RuleChain | 规则链 CRUD、输出标签、根链、元数据更新、调试、脚本测试、导出/导入、edge 分配/模板 | `.../controller/RuleChainController.java` |
| CalculatedField | 计算字段 CRUD、按实体查、按名称查、调试、脚本测试 | `.../controller/CalculatedFieldController.java` |

> 借鉴价值：Queue 对齐 RoseCloud RabbitMQ 抽象；RuleChain/CalculatedField 属"复杂规则引擎"，路线图明确第一阶段不做满，仅作演进参考。

## B11 IoT 专属能力（RoseCloud 基本不适用）

| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| Device/DeviceProfile | 设备 CRUD+凭据；设备配置（默认配置、传输类型、遥测/属性键） | `DeviceController.java`、`DeviceProfileController.java` |
| Asset/AssetProfile | 资产 CRUD+配置 | `AssetController.java`、`AssetProfileController.java` |
| Telemetry | 时序与属性（client/shared/server scope）读写、按 scope/keys 查、TTL、删除 | `.../controller/TelemetryController.java` |
| RPC | one-way/two-way RPC、持久化 RPC 查询/删除 | `RpcV1Controller.java`、`RpcV2Controller.java` |
| OTA | 固件/软件包管理（info/data 分离、按设备配置+类型查） | `.../controller/OtaPackageController.java` |
| DeviceConnectivity | 发布遥测命令、服务器证书下载、网关 docker-compose | `.../controller/DeviceConnectivityController.java` |
| Edge | 边缘节点（assign 实体、root rule chain、同步） | `.../controller/EdgeController.java`、`EdgeEventController.java` |
| LwM2M/IoT Hub/Trendz | 轻量 M2M、IoT Hub 安装/注册、Trendz 分析设置 | `Lwm2mController.java`、`IotHubController.java`、`TrendzController.java` |

> 借鉴价值：均为 IoT 设备管理专属，RoseCloud 作为企业后台/SaaS 底座基本不适用；仅"Profile（类型模板）"与"凭据管理"模式可类比参考。

## B12 集成与扩展

| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| 实体版本控制 | save version、状态查询、list versions/entities、compare、load、branches（git 化配置版本） | `.../controller/EntitiesVersionControlController.java` |
| AutoCommit | 自动提交设置 | `.../controller/AutoCommitController.java` |
| Mobile 应用 | app CRUD、bundle CRUD+OAuth2 绑定、QR 设置、deep link、QR 登录换 token、asset links/apple association | `MobileAppController.java`、`MobileAppBundleController.java`、`QrCodeSettingsController.java` |
| AiModel | AI 模型管理（CRUD + chat） | `.../controller/AiModelController.java` |

> 借鉴价值：VersionControl 可作 RoseCloud 配置/实体版本管理参考；Mobile（QR 登录、bundle+OAuth2）与 AiModel（RoseCloud AI 可选模块）按需借鉴。

## B13 租户管理（Tenant / TenantProfile 套餐配额）

> 租户是顶层隔离单元；`TenantProfile`（套餐）承载配额、限流与 TTL，对应 RoseCloud 的"租户 + 套餐/配额"（§4.11）。

**Tenant（租户）**
| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| 查询租户 | Get Tenant / Tenant Info | `.../controller/TenantController.java`(:78,:92) |
| 租户 CRUD | Create/Update/Delete Tenant（含绑定 `tenantProfileId`） | 同上(:105,:121) |
| 分页查询 | Get Tenants / Tenants Info（系统级） | 同上(:134,:152) |
| 批量查询 | Get Tenants by ids | 同上(:187) |

Tenant 字段：`title`、`region`、`tenantProfileId`、`version`（`.../common/data/Tenant.java`）

**TenantProfile（套餐/配置）**
| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| 查询套餐 | Get Profile / Info / Default | `.../controller/TenantProfileController.java`(:75,:87,:99) |
| 套餐 CRUD | Create/Update/Delete Profile | 同上(:107,:198) |
| 设为默认 | Make default | 同上(:211) |
| 分页/列表 | Get profiles / infos / list | 同上(:225,:243,:269) |

TenantProfile 字段：`name`、`description`、`isDefault`、`isolatedTbRuleEngine`、`profileData`（`.../common/data/TenantProfile.java`）

**DefaultTenantProfileConfiguration（套餐配额/限流/TTL）—— 高价值借鉴**
- 实体配额：`maxDevices/maxAssets/maxCustomers/maxUsers/maxDashboards/maxRuleChains/maxEdges/maxResourcesInBytes/maxOtaPackagesInBytes/maxResourceSize`
- 执行配额：`maxTransportMessages/maxTransportDataPoints/maxREExecutions/maxJSExecutions`
- 限流：`transport*(Tenant/Device/Gateway) MsgRateLimit/TelemetryMsgRateLimit/TelemetryDataPointsRateLimit`、`tenantEntityExport/ImportRateLimit`、`tenantNotificationRequests(RateLimit/PerRuleRateLimit)`、`edgeUplinkMessagesRateLimits(PerEdge)`
- TTL：`defaultStorageTtlDays/alarmsTtlDays/rpcTtlDays/queueStatsTtlDays/ruleEngineExceptionsTtlDays`
- `warnThreshold`（配额预警阈值）

> 借鉴价值：`DefaultTenantProfileConfiguration` 是 RoseCloud 套餐/配额的直接参考——`maxUsers` 等实体配额 + 限流 + TTL + `warnThreshold` 配额预警，可对齐 §4.11"可用性、用户数上限、基础功能开关"。RoseCloud 现 `sys_tenant` + 套餐占位，可据此细化配额模型。

## B14 客户管理（Customer）

> `Customer` 是租户下的子分组（客户），用于把设备/资产/看板分配给特定客户群并做数据隔离。

| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| 查询客户 | Get Customer / short info / title | `.../controller/CustomerController.java`(:85,:102,:120) |
| 客户 CRUD | Create/Update/Delete Customer | 同上(:135,:157) |
| 分页查询 | Get Tenant Customers | 同上(:172) |
| 按标题查 | Get Tenant Customer by title | 同上(:193) |
| 批量查询 | Get customers by ids | 同上(:218) |

Customer 字段：`title`、`tenantId`、`externalId`、`version`（`.../common/data/Customer.java`）

> 借鉴价值：RoseCloud 多租户以 Tenant 为顶层；"Customer（租户下客户分组）+ 实体 assign 给 customer" 可作为组织/部门/项目层级的参考模型（若 RoseCloud 需要租户内再分组）。

## B15 设备管理（Device / DeviceProfile）

> 设备是 IoT 核心实体；`DeviceProfile` 是设备类型模板（传输/配置/告警/开通）。RoseCloud 基本不适用，但"Profile 模板 + 实体 + 凭据 + assign" 模式可类比。

**Device（设备）**
| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| 查询设备 | Get Device / Device Info | `.../controller/DeviceController.java`(:144,:159) |
| 设备 CRUD | Create/Update/Delete Device | 同上(:174,:205,:241) |
| 建设备带凭据 | Create Device with credentials | 同上(:205) |
| 分配/公开 | Assign/Unassign to customer / Make public | 同上(:254,:272,:291) |
| 凭据管理 | Get/Update Device Credentials | 同上(:306,:319) |
| 分页查询 | Get Tenant/Customer Devices + infos | 同上(:351,:434,:378,:466) |
| 多维查询 | by name / by ids / find by query / get types | 同上(:423,:507,:531,:557) |

Device 字段：`tenantId`、`customerId`、`name`、`type`、`label`、`deviceProfileId`、`deviceData`、`firmwareId`、`softwareId`（`.../common/data/Device.java`）

**DeviceProfile（设备类型模板）**
| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| 查询/默认 | Get Profile / Info / Default | `.../controller/DeviceProfileController.java`(:89,:109,:123) |
| 查询配置键 | Get timeseries/attribute keys | 同上(:133,:156) |
| CRUD/默认 | Create/Update/Delete / Make default | 同上(:179,:198,:213) |
| 分页/批量 | profiles/infos(by transport)/names/by ids | 同上(:228,:248,:270,:296) |

DeviceProfile 字段：`name`、`description`、`image`、`isDefault`、`type`、`transportType`(`DEFAULT/MQTT/COAP/LWM2M/SNMP`)、`provisionType`、`defaultRuleChainId`、`defaultDashboardId`、`defaultQueueName`、`profileData`、`provisionDeviceKey`、`firmwareId`、`softwareId`、`defaultEdgeRuleChainId`
DeviceProfileData：`configuration`、`transportConfiguration`、`provisionConfiguration`、`alarms`(List<DeviceProfileAlarm>)

> 借鉴价值：基本不适用（IoT 设备专属）；"Profile 类型模板（含默认配置/告警模板/开通策略）+ 实体绑定 Profile" 模式可类比 RoseCloud 的资源类型模板。

## B16 资产管理（Asset / AssetProfile）

> 资产是设备的逻辑分组/业务对象（如车间、产线）；`AssetProfile` 为资产类型模板。

**Asset（资产）**
| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| 查询资产 | Get Asset / Asset Info | `.../controller/AssetController.java`(:108,:123) |
| 资产 CRUD | Create/Update/Delete Asset | 同上(:138,:160) |
| 分配/公开 | Assign/Unassign to customer / Make public | 同上(:172,:188,:204) |
| 分页查询 | Get Tenant/Customer Assets + infos | 同上(:218,:245,:296,:328) |
| 多维查询 | by name / by ids / find by query / get types | 同上(:285,:365,:389,:413) |
| 边缘分配 | Assign/Unassign to edge / Get edge assets | 同上(:426,:449,:471) |

Asset 字段：`tenantId`、`customerId`、`name`、`type`、`label`、`assetProfileId`（`.../common/data/Asset.java`）

**AssetProfile（资产类型模板）**
| 功能 | 说明 | ThingsBoard 位置 |
|---|---|---|
| 查询/默认 | Get Profile / Info / Default | `.../controller/AssetProfileController.java`(:82,:102,:116) |
| CRUD/默认 | Create/Update/Delete / Make default | 同上(:126,:145,:160) |
| 分页/批量 | profiles/infos/names/by ids | 同上(:175,:195,:215,:243) |

AssetProfile 字段：`name`、`description`、`image`、`isDefault`、`defaultRuleChainId`、`defaultDashboardId`、`defaultQueueName`、`defaultEdgeRuleChainId`、`externalId`（`.../common/data/AssetProfile.java`）

> 借鉴价值：基本不适用；"资产=业务对象分组 + 关联设备/看板" 的逻辑分组思路可类比 RoseCloud 的资源分组/组织树。

## B17 客户与临时令牌登录（新方向借鉴）

> 方向：RoseCloud 在租户下引入"客户"（Customer），客户可申请临时令牌登录。对照 ThingsBoard 的 Customer + token access + public login，明确可借鉴与需自研的边界。

**ThingsBoard 现有机制（源码）**
- Customer 实体：租户下子分组，`CUSTOMER_USER` authority（见 B14、`Authority` 枚举）。
- Token access（管理员代登录）：`/user/tokenAccessEnabled` + `/user/{userId}/token`（`UserController.java`:155、:160）——`SYS_ADMIN` 可代任一租户管理员、`TENANT_ADMIN` 可代本租户任一客户用户；返回**完整 JWT pair**（`createTokenPair`），**无独立短 TTL**，由 `userTokenAccessEnabled` 开关门控。
- Public login（公开访问）：`PublicLoginRequest(publicId)` + `RestPublicLoginProcessingFilter` + `authenticateByPublicId`——按 publicId 登录公共客户，用于公开看板，**非客户自助令牌**。

**借鉴与自研边界（关键）**
- ThingsBoard **没有**"客户自助申请短 TTL 可吊销令牌"的现成机制：token access 是**管理员发起**的代登录、发完整 pair、无独立 TTL。
- RoseCloud"客户申请临时 token 登录"需自研扩展：
  - 自助申请（客户发起，非管理员代发）
  - 短 TTL + 可吊销（复用 jti 吊销，见 A1）
  - scope 受限（仅客户可访问范围，非完整权限）
  - 申请限频 / 审批（可选）
- 可直接借鉴：Customer 实体模型（B14）、token access 的"为指定用户发 JWT"思路、`userTokenAccessEnabled` 开关门控模式。

**与 RoseCloud 角色体系对齐**
- RoseCloud §4.4 现有角色无"客户"。引入 Customer 需在 01 §4.4 / §4.9 裁定：Customer 是租户下**新实体 + `CUSTOMER_USER` 角色**（区别于普通业务用户），还是 Customer = 普通业务用户的租户内分组。本 doc 不替 01 决定，仅标明缺口。

> 借鉴价值：Customer 实体 + token-access 思路 + 开关门控可借鉴；"自助临时令牌（短 TTL / 可吊销 / scope 受限）"为 RoseCloud 自研扩展，ThingsBoard 无现成方案。

# Part C 风险取舍与结论

## 风险与取舍
- **默认关闭**：MFA/OAuth2 客户端均以 `rosecloud.{name}.enabled=true` 门控，关闭时零装配，符合 §5.4；不可让默认路径回归。
- **双模式兼容**：认证增强须在微服务与单体均可用——OAuth2 客户端登录、pre-auth 令牌校验在 `rosecloud-auth`/`MonolithJwtFilter` 两端一致。
- **不引入过度抽象**：MFA provider SPI、OAuth2 mapper SPI 是稳定接缝；其余策略以最小改动实现。
- **依赖方向**：MFA/OAuth2 客户端 starter 依赖 `rosecloud-api`+`common-security`+`starter-security-jwt`，禁止反向；SMS/Email 经 Feign/`NoticeChannelSender` 调 notice。
- **OAuth2↔MFA 一致性**：ThingsBoard 本版本 OAuth2 登录不触发 MFA；RoseCloud 若要统一安全策略需在成功处理器显式补 `isTwoFaEnabled` 分支。
- **密钥管理**：ThingsBoard 配置入库 + `reload()`；RoseCloud 现走 Nacos 共享配置跨实例一致性已较好，按需权衡是否再入库。

## 结论
- **认证**：ThingsBoard 最值得借鉴——JWT `sessionId` 会话治理、MFA Provider SPI + pre-auth 令牌、OAuth2 客户端登录(DB 动态注册 + mapper SPI + Cookie 仓库)。三者均可在 RoseCloud 现有 starter/JWT/notice/会话表上低成本落地，与"默认关闭但预留、P1、双模式兼容"路线图一致。
- **平台能力**：系统/用户/通知可参考清单中，RoseCloud v1 已覆盖主线（用户/租户/任务/会话/登录日志/审计/通知站内+邮件+短信）；增强方向为：平台功能开关 API、改密失效与登录锁定、用户激活/重置令牌、通知规则自动化与用户偏好。其余模块中，**统一实体+关系图/EntityView（数据权限）、告警闭环、看板组件、Job cancel/reprocess、资源文件 ETag、实体版本控制**为高价值借鉴点；IoT 专属能力（设备/遥测/RPC/Edge 等）基本不适用。
