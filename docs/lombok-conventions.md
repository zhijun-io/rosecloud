# Lombok 使用规范

本文档规定 `rosecloud` 项目中 Lombok 与 `record` 的选型、写法纪律与验证要求。它是对
`AGENTS.md` Conventions 段「注解选型」的细化与权威说明。

> 一句话：字段多不是用不用 `record` 的决定因素——决定因素是「这个对象要不要可变、要不要
> 继承、要不要被 ORM 实例化、要不要选择性 `equals`、要不要实现 JavaBean 风格的接口」。

---

## 1. 基础设施（已配置，新增模块无需重复声明）

- 根 `pom.xml`：`dependencyManagement` 中以 `provided` 引入 Lombok；根依赖包含 Lombok；
  `maven-compiler-plugin` 的 `annotationProcessorPaths` 已加入 Lombok 与
  `spring-boot-configuration-processor`。
- `rosecloud-bom/pom.xml`：同步声明，供外部消费者使用。
- **新增模块若需 Lombok 直接使用即可，无需再声明版本或依赖。**

---

## 2. 注解选型决策表

| 对象特征 | 用 | 理由 |
| --- | --- | --- |
| 不可变数据 + 由 JSON 反序列化 + 无行为（API / 请求响应 DTO、纯值对象） | **`record`** | 零依赖、简洁、Jackson 原生支持；访问器为 `xxx()` |
| 不可变载体、**不**实现 JavaBean 接口、也无 builder/with 需求 | **`record`** | 同上，最轻 |
| 不可变 + 实现 `HasId`/`HasTenantId`/`HasUserId`/`HasStatus` 等接口，或需要 `@With`/`@Builder`/选择性 `equals` | **Lombok `@Value`** | Lombok 生成 `getXxx()` 风格访问器，能满足接口（见第 4 节禁令） |
| 可变 / 需 no-arg 构造器 / 继承 `BaseEntity` / 被 ORM 框架填充 | **`@Getter @Setter @NoArgsConstructor`** | Entity 层唯一选项 |
| 可变且需要全部 `get/set/equals/hashCode/toString` | **`@Data`** | 但要警惕 Entity 的 `equals` 陷阱，实体一般不用 |
| Spring bean（`@Service`/`@Component`/`@RestController` 等，构造器注入依赖） | **`@RequiredArgsConstructor`** | Lombok 为全部非静态 `final` 字段生成构造器，Spring 单构造器自动注入；增删依赖只改字段（详见第 8 节） |

> 项目分层现状（已符合上表）：`rosecloud-api` 与 `service/dto` 的 DTO → `record`；
> Domain 层不可变载体 → `@Value`；Entity 层（`extends BaseEntity`）→
> `@Getter @Setter @NoArgsConstructor`。

---

## 3. `@Value` 写法纪律（重点）

- `@Value` 生成不可变字段 + `getXxx()` 访问器 + 全参构造器 + `equals`/`hashCode`/`toString`，
  **无 setter**。调用点要「改字段」时用 `withXxx()` 复制方法返回新副本，而非 `setXxx()`。
- **`@Value` + 短调用点 = 必须同时手写全参构造器**：
  手写任意构造器会抑制 Lombok 自动生成的全参构造器。若存在短构造（如 17 参）委托全参
  （如 21 参），必须**显式手写全参构造器**，否则 `this(...)` 委托失败、编译报
  `找不到符号`。
- **切勿在 `@Value` 上再叠 `@Data` / `@AllArgsConstructor`**：与 `final` 字段及已生成的
  构造器冲突，且缺 `import lombok.Value` 会 `找不到符号`。
- **不想写双构造器时**：改用 `static of(...)` 工厂 + 仅保留 `@Value` 自动生成的全参构造，
  把调用点统一成工厂或全参形式。
- 多参 `@Value` 需要命名构造 / 部分字段构造时，加 `@Builder`（与 `@Value` 兼容）。

---

## 4. 关键禁令：`record` 不能实现 JavaBean 风格的 getter 接口

- 项目的 `HasId` / `HasTenantId` / `HasUserId` / `HasStatus<S>` 接口是 **JavaBean 风格**
  （声明 `Long getId()`、`String getTenantId()`、`Long getUserId()`、`S getStatus()`）。
- **Java `record` 的访问器是 `id()` / `tenantId()` / `userId()` / `status()`，不是
  `getId()` 等**。因此 `record` 生成的方法**不会覆盖**接口的 `getId()`，编译报
  `不是抽象的, 并且未覆盖 ... 中的抽象方法 getId()`，且所有 `n.getId()` 调用点
  `找不到符号`。
- **结论：凡实现上述接口的不可变载体，必须用 `@Value`（Lombok 生成 `getXxx()`），
  绝不能用 `record`。** 这与第 2 表「实现接口 → `@Value`」一致。
- 推而广之：**访问器一律 getter 风格 `getXxx()`**，保持与接口一致；不要写 `record` 风格的
  `xxx()` 访问器去对接 JavaBean 接口。

---

## 5. 验证纪律

- Lombok 改造（改注解、改构造器、加/删 `@Value` 类）后，**必须跑一次全新编译**
  （`clean compile` / `clean test-compile`，或对上游模块 `-am compile` / `-am test`），
  不能用增量 `test-compile` 判断。
- 原因：增量构建会跳过未发生内容变更的 `target/classes`，可能让「改坏却未重编」的文件
  呈现「假绿」。本项目 `Notice`/`NoticeRecord` 的 `@Value` 改造即曾因增量编译漏网。

---

## 6. 反模式清单（不要用）

1. 在 Entity 上用 `@Data` —— 全字段 `equals` 陷阱；应 `@Getter @Setter @NoArgsConstructor`。
2. 对实现 `HasId` / `HasTenantId` / `HasUserId` / `HasStatus` 的类用 `record` —— 接口不兼容。
3. 在 `@Value` 上叠 `@Data` / `@AllArgsConstructor` —— 构造器与 `final` 字段冲突。
4. `@Value` 类手写短构造却不手写全参构造 —— `this(...)` 委托失败。
5. 改完 Lombok 只跑增量编译就当验证通过 —— 可能「假绿」。
6. 用 `record` 风格 `xxx()` 访问器去对接声明 `getXxx()` 的接口。
7. 手写 Spring bean 构造器做纯依赖注入 -- 应 `@RequiredArgsConstructor` + `private final` 字段；构造器含派生字段（如从 `Properties` 算出 `final long`）时除外（见第 8 节）。

---

## 7. 示例

### 7.1 正确的不可变 DTO（`record`，无接口）

```java
public record UserCreateRequest(
        String username,
        String nickname,
        String tenantId
) {}
```

### 7.2 正确的不可变 Domain（实现 `HasId` 接口 → 必须 `@Value`）

```java
@Value
public class NoticeRecord implements HasId, HasUserId, HasTenantId {
    Long id;
    Long noticeId;
    Long userId;
    String tenantId;
    // ... 其余字段
}
// Lombok 自动生成 getId()/getUserId()/getTenantId()，满足接口
```

### 7.3 `@Value` 含全参构造 + `withXxx` 复制（带空守卫）

```java
@Value
public class Notice implements HasId, HasStatus<Integer>, HasTenantId {
    Long id;
    String title;
    // ... 其余字段
    List<NoticeRecipient> recipients;

    // 手写全参构造器（Lombok 因有显式构造不再生成；此处做空守卫）
    public Notice(/* 全参 */) {
        // ... 赋值 ...
        this.recipients = recipients == null ? List.of() : List.copyOf(recipients);
    }

    /** 置 id 的不可变复制 */
    public Notice withId(Long id) {
        return new Notice(id, /* 其余字段 */);
    }
}
```

### 7.4 Entity（可变，被 ORM 填充）

```java
@Getter
@Setter
@NoArgsConstructor
public class UserEntity extends BaseEntity {
    String username;
    String nickname;
}
```

---

## 8. Spring 依赖注入：用 `@RequiredArgsConstructor`

> 一句话：Spring bean 的构造器注入用 `@RequiredArgsConstructor`，不要手写构造器。这不替代
> `record`/`@Value`--后者管数据载体，本节管带依赖的行为对象。

- Spring bean（`@Service` / `@Component` / `@RestController` / `@Configuration` / `@Repository` 等）
  的构造器注入一律用 Lombok `@RequiredArgsConstructor`，**不要手写构造器**。
- 依赖声明为 `private final` 字段，Lombok 为全部**非静态 `final`** 字段生成构造器；Spring 4.3+
  单构造器自动注入，**无需 `@Autowired`**。
- 增删依赖只增删 `final` 字段，构造器与赋值由 Lombok 同步，避免手写构造器随字段变更漏改。
- **与 `record` 不冲突**：`record`/`@Value` 用于不可变数据载体（DTO、Domain 值对象）；
  `@RequiredArgsConstructor` 用于 Spring bean。判据是「对象是什么」，不是「用哪个注解家族」。
  不要把 `@RequiredArgsConstructor` 套到 `record` 上（record 自带规范构造器），也不要用 `record`
  当 Spring 服务 bean。
- **例外**：构造器需要派生/计算字段时仍手写构造器。例如 `final long activationTtlHours` 由
  `properties.getActivationTtlHours()` 算出而非注入--此时套 `@RequiredArgsConstructor` 会把派生
  字段当成注入参数，应保留手写构造器（如 `UserActivationServiceImpl`）。

### 8.1 正确：`@RequiredArgsConstructor` 构造器注入

```java
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantProvisioner {
    private static final String TENANT_ADMIN_ROLE_CODE = "tenant-admin";

    private final TenantMapper tenantMapper;
    private final RoleMapper roleMapper;
    private final UserService userService;
    private final UserActivationService userActivationService;
    // Lombok 生成全参构造器，Spring 自动注入；增删依赖只改 final 字段
}
```

### 8.2 反模式：手写构造器做纯依赖注入

```java
// 反模式：增删依赖都要同步改构造器
@Component
public class TenantProvisioner {
    private final TenantMapper tenantMapper;
    private final RoleMapper roleMapper;
    private final UserService userService;
    private final UserActivationService userActivationService;

    public TenantProvisioner(TenantMapper tenantMapper, RoleMapper roleMapper,
                             UserService userService, UserActivationService userActivationService) {
        this.tenantMapper = tenantMapper;
        this.roleMapper = roleMapper;
        this.userService = userService;
        this.userActivationService = userActivationService;
    }
}
```
