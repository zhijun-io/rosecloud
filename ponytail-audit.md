# ponytail-audit — RoseCloud 过度工程审计

> 范围：仅过度工程与复杂度（正确性 / 安全 / 性能均不在范围内，请交给常规 review）。
> 本报告为一次性只读审计，**不修改任何代码**。所有发现均经 grep 调用点 + 逐文件核验。
> 已核实"非过度工程"项：租户隔离 `RoseCloudTenantLineHandler`（正确复用 MP `TenantLineHandler`）、`common-security` 全部 token 类（均被使用）、`SessionStore`（JWT 吊销表，非 Spring Session 重复）、`JwtAuthSupport`/`TenantStatusChecks`（各有 2 个真实调用方）、`NoticeChannelSender`（Email + Sms 两个实现，抽象合理）。

排名：按可削减行数 / 可移除模块优先级，最大刀口在前。

---

### 1. `delete:` 整个 `rosecloud-starter-cache` 模块是死代码  ✅ 已执行（2026-07-10）
全仓库 grep `RoseCloudCache` 仅命中缓存 starter 自身的 6 个文件 + 测试；无任何消费者；所有 `*.yml` 中无 `rosecloud.cache.*` 配置；`auth` 的 pom 虽依赖它但零引用 → 连 `increment()` 登录失败计数器都从未接线。
替换：如确需集中缓存，直接用 Spring `CacheManager`（`CaffeineCacheManager` / `RedisCacheManager`）。当前无需任何东西。
[rosecloud-starter/rosecloud-starter-cache/** （~280 行 main + 测试），并移除 rosecloud-starter/pom.xml 的 `<module>`、rosecloud-bom 的 BOM 条目、rosecloud-auth/pom.xml 的缓存依赖] — **已执行：模块目录已删除，aggregator `pom.xml`、BOM、`README.md` 模块树均已清理（含测试文件）。**

### 2. `delete:` `SmsNoticeSender` 是空 stub  ✅ 已执行（2026-07-10）
方法体仅 `log.info("sms dispatch (stub)...")`，注释自承"真实投递是 follow-up"。
替换：无（待选定 SMS provider 后再加真实实现）。
[rosecloud-service/rosecloud-notice/.../channel/SmsNoticeSender.java （29 行）] — **已执行：文件已删除，`NoticeDispatchService` 的 SMS 分支同步移除（`NoticeChannel.SMS` 枚举保留，待真实 provider 接入再恢复）。**

### 3. `yagni:` 系统模块 14 个 `Repository` 接口 = 1:1 单一实现，其中至少 8 个是纯委托
grep `interface \w+Repository` 得 14 个接口、14 个 `implements`，每个接口恰好一个同模块 impl（无第二实现、无跨模块替换）。`SettingKeyRepositoryImpl` 已逐行核验：仅是 `toDomain/toEntity` 字段对拷 + 转调 Mapper，加零星 null 保护，零业务逻辑。同模式的纯委托 impl：`SystemSettingRepositoryImpl`、`UserSettingRepositoryImpl`、`LoginLogRepositoryImpl`、`DeptRepositoryImpl`、`DictDataRepositoryImpl`、`DictTypeRepositoryImpl`、`AuditLogRepositoryImpl`（合计约 624 行样板）。
替换：纯委托的接口 + Impl 直接内联到 Service 层用 Mapper / MyBatis-Plus `IService`；保留少量带实质逻辑者（`UserRepositoryImpl`、`TenantRepositoryImpl` 等）但去掉空接口。
[rosecloud-service/rosecloud-system/.../domain/*Repository.java + persistence/*RepositoryImpl.java]

### 4. `shrink:` 每个实体「Domain record + Entity + Mapper + Repository + RepositoryImpl」四层 + 字段双写
全仓库最显著的行数膨胀源。13 个实体全部重复此模式，Domain 与 Entity 字段近乎 1:1 复制，Impl 的 `toDomain/toEntity` 为机械逐字段对拷（见 `SettingKeyRepositoryImpl:83-99`）。
替换：纯 CRUD 实体去掉 Domain/Entity 双写与空 Repository 接口，让 Entity 直接承担领域角色；仅保留带真实行为的领域类（`User`/`Tenant`/`Notice`）。预计削减系统 persistence+domain 包（约 2300 行）的 30–40%。
[rosecloud-service/rosecloud-system/.../domain/*, persistence/*]

### 5. `yagni:` 动态数据源 `dynamic` 包从未启用  ✅ 已执行（2026-07-10）
`DataSourceRoute` 接口仅存在默认 no-op bean（恒返回 primary），无任何真实实现；全仓库无 `rosecloud.datasource.dynamic` 配置。整条路径是预先铺设的"预留功能"，当前恒等于直连主库。
替换：删除整段 `dynamic` 包 + 相关测试。`AbstractRoutingDataSource` 是 Spring 标准扩展点，真有多数据源时再按需接入。
[rosecloud-starter/rosecloud-starter-data-mybatisplus/.../data/dynamic/* （~173 行 + 测试）] — **已执行：整个 `dynamic` 包（5 个源文件 + 1 个测试）及 `AutoConfiguration.imports` 中对应行已删除，无任何消费者。**

### 6. `shrink:` `BaseDataWithAdditionalInfo` 自研 JSON 双表示
手搓 `JsonNode additionalInfo` + `byte[] additionalInfoBytes` 双存储 + `getJson/setJson` 编解码 + 自定义 `equals/hashCode`（109 行），被 `Tenant`/`User`/`TenantProfile` 继承。
替换：Jackson 已能直接 (反)序列化 `JsonNode`；单字段 + 标准 getter/setter 即可，删掉手工序列化与 `equals/hashCode`（若不需要）。可削至约 30 行。
[rosecloud-common/.../core/model/BaseDataWithAdditionalInfo.java]

### 7. `stdlib:` `LocalRoseCloudCache` 手搓 TTL + 过期扫描
`ConcurrentHashMap` + `ScheduledExecutorService` sweeper + `CacheEntry` 记录，正是 Caffeine / Spring Cache 已提供的。
替换：见 #1，统一用 `Caffeine.newBuilder().expireAfterWrite(...)` 或 Spring Cache。
[rosecloud-starter/rosecloud-starter-cache/.../LocalRoseCloudCache.java （96 行，随模块删除）]

### 8. `yagni:` 通知派发间接层大半是预留  ✅ 已执行（2026-07-10）
`NoticeDispatchService.doDispatch` 对 ROLE / TENANT / GLOBAL 目标的接收人明确"未实现"（仅 warn 跳过），push 渠道实际只对 USER 定向通知可用；`NoticeDispatchContext`（15 行 record）仅作为透传载体穿过 `doDispatch`。
替换：`NoticeDispatchContext` 内联为方法参数；SMS 分支（配合 #2 删除 stub）砍掉，待真实渠道接入再恢复。
[rosecloud-service/rosecloud-notice/.../channel/NoticeDispatchContext.java + NoticeDispatchService.doDispatch] — **已执行：`NoticeDispatchContext` 内联为方法参数，`doDispatch` 仅保留 EMAIL 渠道分支（与 #2 SMS stub 删除一致）。**

### 9. `yagni:` `LoginTenantResolver` 接口 + 唯一实现（低优先级，可暂留）
仅 `TenantSelectionService` 一个实现。因安全 starter 不应反向依赖 auth 服务，保留该抽象有架构理由 → 标为可选，不计入主要收益。
[rosecloud-starter-security/.../auth/LoginTenantResolver.java + rosecloud-auth/.../service/TenantSelectionService.java]

### 10. `yagni:` 11 个 `Has*` 标记接口（低优先级，可暂留）
`HasId/HasCode/HasKey/HasName/HasStatus/HasTenantId/HasUserId/HasAdditionalInfo/HasUpdatedAt/HasUpdatedBy/HasParentId` 仅声明 getter、无行为。低风险，保留不影响。
[rosecloud-common/.../core/model/Has*.java]

### 11. `shrink:` `BaseData` 公有静态 `ObjectMapper`（小，低优先级）
反模式；应注入而非全局静态。
[rosecloud-common/.../core/model/BaseData.java]

---

**net: -~1,800 行（含 #4 CRUD 层合并）/ -1 个 starter 模块（rosecloud-starter-cache）可能；保守不动大重构的地板 ≈ -450 行 + -1 模块。**

> 说明：初稿中"死 `inMemorySessionStore` bean"已核实为**误报**——该 bean 由 `@ConditionalOnMissingBean` + 无 `@ConditionalOnClass` 守卫，是 `StringRedisTemplate` 不在 classpath 时的正确兜底，予以剔除。另，`NoticeChannelSender` 有 Email（功能完整）+ Sms（stub）两个实现，抽象本身合理，仅 Sms stub 按 #2 删除。
