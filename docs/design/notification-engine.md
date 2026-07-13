# RoseCloud 通知引擎设计方案

> 版本 v23（deep-module 评审：收紧 `DeliveryResult` 构造可见性）-- 从"可插拔通知基础设施框架"收缩为"进程内最小投递内核"。

## 0. 定位与不可约内核

先回答"这个引擎的不可约内核到底是什么"：

> **把一条通知，在一次调用生命周期内按固定策略投递给若干收件人和渠道；在有限超时内完成重试；输出稳定、可记录的投递结果。**

这个定义刻意不再扩成"小型通知基础设施框架"。首版只解决以下问题：

- 调用方已经拿到了收件人和渠道地址。
- 调用方已经决定要不要调用这个引擎。
- 引擎只负责在当前进程里把通知交给渠道并汇总结果。

因此，以下能力明确不属于首版内核：

- 持久化
- 崩溃恢复
- 收件人解析
- 分布式限流
- 熔断器
- 插件发现
- 自动路由规则 DSL

装配原则也同步收缩：**显式构造优先，不做自动发现**。

硬边界：框架无关、无 CDI/Spring；并发仅用标准 `java.util.concurrent`；不负责持久化、不负责收件人解析、不负责崩溃恢复；仅依赖标准 JDK；包根 `io.rosecloud.notification.*`。

---

## 1. 架构与分层

```text
io.rosecloud.notification
├── model/         不可变数据载体（Notification, Recipient, 结果, 枚举）
├── core/          NotificationEngine、Builder、DeliveryPolicy、执行骨架
└── channel/       NotificationChannel 及渠道实现
```

首版只保留四类概念：

- **输入模型**：通知、收件人、渠道类型
- **固定投递策略**：fan-out 或 failover、最大重试次数、超时、全局并发
- **渠道适配**：真正发送通知的实现
- **结果输出**：稳定、结构化的投递结果

### 1.1 关键决策

| 决策 | 原因 |
|---|---|
| 不再开放 `RoutingStrategy` SPI | 首版只有 `FAN_OUT` 与 `FAILOVER` 两种固定策略，不值得抽象成子系统。 |
| 不再开放 `RateLimiter` / `RetryPolicy` / `CircuitBreaker` SPI | 这些都是通用 resilience 轮子，不属于通知引擎 v1 最小问题域。 |
| 不再使用 `ServiceLoader` | 首版没有真实插件生态，自动发现只会扩大测试面和约束面。 |
| 保留 `NotificationChannel` 作为唯一必须扩展点 | 发送动作天然因渠道而异；这是首版唯一真正不可避免的变点。 |
| 不再保留 observer SPI | v1 的不可约输出是 `BroadcastResult`；attempt 级观测不是首版内核职责。 |
| 不把幂等放进 v1 | 幂等本质上依赖持久化与跨调用协议，超出"单次进程内投递"的边界。 |

---

## 2. 核心接口与契约

### 2.1 数据模型

```java
/**
 * 渠道无关的通知载体。
 * 引擎只关心稳定 ID、渠道无关 payload 和目标渠道。
 */
public record Notification(
        String notificationId,
        Map<String, Object> payload,
        Set<ChannelType> targetChannels) {
    public Notification {
        if (notificationId == null || notificationId.isBlank()) {
            throw new IllegalArgumentException("notificationId must not be blank");
        }
        if (!notificationId.equals(notificationId.trim())) {
            throw new IllegalArgumentException("notificationId must not have surrounding whitespace");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        if (targetChannels == null) {
            throw new IllegalArgumentException("targetChannels must not be null");
        }
        payload = Map.copyOf(payload);
        targetChannels = Set.copyOf(targetChannels);
    }
}

/**
 * 收件人视图。地址由调用方提前解析好，引擎不负责查人、查租户、查角色。
 * context 是收件人维度的透传上下文，引擎不解释其语义。
 */
public record Recipient(
        String recipientId,
        Map<ChannelType, String> addresses,
        Map<String, Object> context) {
    public Recipient {
        if (recipientId == null || recipientId.isBlank()) {
            throw new IllegalArgumentException("recipientId must not be blank");
        }
        if (!recipientId.equals(recipientId.trim())) {
            throw new IllegalArgumentException("recipientId must not have surrounding whitespace");
        }
        if (addresses == null) {
            throw new IllegalArgumentException("addresses must not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        addresses = Map.copyOf(addresses);
        context = Map.copyOf(context);
    }
}

/**
 * 渠道类型。
 */
public record ChannelType(String name) {
    public ChannelType {
        if (name == null) {
            throw new IllegalArgumentException("channel type name must not be null");
        }
        name = name.trim().toUpperCase(java.util.Locale.ROOT);
        if (!name.matches("[A-Z][A-Z0-9_]*")) {
            throw new IllegalArgumentException("invalid channel type name: " + name);
        }
    }

    public static final ChannelType EMAIL = new ChannelType("EMAIL");
    public static final ChannelType SMS   = new ChannelType("SMS");
}
```

`ChannelType` 合同：

- `name` 参与判等、排序和注册表查找，因此不能是自由文本。
- 规范化规则固定为：`trim()` 后转成 `Locale.ROOT` 下的大写。
- 合法格式固定为正则 `[A-Z][A-Z0-9_]*`。
- 因此 `email`、` EMAIL `、`Email` 会规范化为同一个渠道类型 `EMAIL`。
- `EMAIL`、`SMS` 是文档内置示例类型；自定义类型与它们遵守同一命名规则，例如 `PUSH`、`WEBHOOK`、`DINGTALK_BOT`。

`notificationId` / `recipientId` 合同：

- `notificationId` 是**通知标识**，用于把一次 `broadcast()` 的输入与 `BroadcastResult` / `DeliveryResult` 关联起来。
- `notificationId` 必须非空、不能只有空白、不能带首尾空白；引擎按**原样大小写敏感**保留，不做规范化。
- 文档只要求 `notificationId` 在一次广播及其结果中稳定不变；是否跨调用全局唯一，由调用方决定。
- `recipientId` 是**收件人业务标签**，用于让调用方把结果映射回自己的收件人条目。
- `recipientId` 也必须非空、不能只有空白、不能带首尾空白；同样按**原样大小写敏感**保留。
- `recipientId` 在单次广播内**必须唯一**；重复 `recipientId` 视为非法输入，引擎在 `broadcast()` 入口抛 `IllegalArgumentException`。这样调用方可直接用 `recipientId` 把结果映射回收件人。

`addresses` 合同：

- v1 只支持"**每个 recipient 每个 channel 最多一个已解析字符串地址**"，因此类型保持为 `Map<ChannelType, String>`。
- `payload`、`targetChannels`、`addresses`、`context` 四个容器字段都必须为**非 null**；若业务语义上"没有内容 / 没有目标渠道 / 没有地址 / 没有上下文"，调用方应传空 `Map` / 空 `Set`，而不是 `null`。
- 这四个容器字段在模型构造时都会做 `Map.copyOf(...)` / `Set.copyOf(...)`，因此进入引擎后可视为**容器结构层面的不可变快照**：调用方不能再通过原始容器引用增删改条目。
- 由于采用 `copyOf(...)`，这些容器里的 key / element / value 也都必须非 null；因此 `addresses` 中的 `null value`、`payload/context` 中的 `null key/value`、`targetChannels` 中的 `null element` 都属于非法输入，而不是运行时分类分支。
- 这不是深拷贝：`payload/context` 中 value object 的内部可变状态不会被冻结或复制。若调用方传入可变对象，必须自行保证在 `broadcast()` 生命周期内不再修改它们。
- 引擎不负责把用户、手机号列表、邮箱列表、topic、机器人 ID 等复杂目标解析成地址；这些都应在进入引擎前收敛成单字符串地址。
- `NO_ADDRESS` 的判定固定为：目标渠道在 `addresses` 中不存在、值为空串、或值只包含空白。
- 对非空白地址，v1 不做通用格式校验；邮箱是否合法、手机号是否合法、URL 是否合法，由对应渠道实现自行决定。
- 地址的业务级规范化责任默认在**调用方**：例如去首尾空白、统一手机号格式、统一邮箱大小写策略。
- 引擎在做 `NO_ADDRESS` 判定时只允许执行最小清洗：把"全空白"视为无地址；除此之外不改写地址值。

`targetChannels` 合同：

- 空 `Set` 表示"未指定目标渠道"，运行时等价于"使用当前已注册的全部渠道"。
- `null` 不再承担任何特殊语义；它只是非法输入。

`Notification.payload` 和 `Recipient.context` 都是**引擎不解释的透传数据**：

- `payload` 表示通知本身的渠道无关内容
- `context` 表示收件人维度的补充上下文

本版刻意不再保留 `attributes` / `variables` 这类额外数据袋，避免在 v1 内核里形成隐含 DSL。若业务需要模板、标签、实验开关等更高层语义，应由调用方在进入引擎前自行归并到 `payload` 或 `context` 中。

### 2.2 固定投递策略

首版不再把"如何投递"拆成多个 SPI，而是收敛为一个固定策略对象：

```java
public record DeliveryPolicy(
        Mode mode,
        List<ChannelType> failoverOrder,
        Duration deadline,
        Duration acquireTimeout,
        int maxAttempts,
        Duration baseBackoff,
        int maxConcurrency) {

    public DeliveryPolicy {
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
        if (failoverOrder == null) {
            throw new IllegalArgumentException("failoverOrder must not be null");
        }
        if (deadline == null) {
            throw new IllegalArgumentException("deadline must not be null");
        }
        if (acquireTimeout == null) {
            throw new IllegalArgumentException("acquireTimeout must not be null");
        }
        if (baseBackoff == null) {
            throw new IllegalArgumentException("baseBackoff must not be null");
        }
        failoverOrder = List.copyOf(failoverOrder);
        if (mode == Mode.FAILOVER && failoverOrder.isEmpty()) {
            throw new IllegalArgumentException("failoverOrder must not be empty in FAILOVER mode");
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        if (maxConcurrency <= 0) {
            throw new IllegalArgumentException("maxConcurrency must be positive");
        }
        if (deadline.isNegative()) {
            throw new IllegalArgumentException("deadline must not be negative");
        }
        if (acquireTimeout.isNegative()) {
            throw new IllegalArgumentException("acquireTimeout must not be negative");
        }
        if (baseBackoff.isNegative()) {
            throw new IllegalArgumentException("baseBackoff must not be negative");
        }
    }

    /** fan-out：所有可投递渠道并发尝试。返回 builder，命名参数避免 Duration/int 位置传错。 */
    public static Builder fanOut() {
        return new Builder(Mode.FAN_OUT, List.of());
    }

    /** failover：按 failoverOrder 串行尝试，首个成功即停。 */
    public static Builder failover(List<ChannelType> failoverOrder) {
        return new Builder(Mode.FAILOVER, List.copyOf(failoverOrder));
    }

    public static final class Builder {
        private final Mode mode;
        private final List<ChannelType> failoverOrder;
        private Duration deadline;
        private Duration acquireTimeout;
        private int maxAttempts;
        private Duration baseBackoff;
        private int maxConcurrency;

        private Builder(Mode mode, List<ChannelType> failoverOrder) {
            this.mode = mode;
            this.failoverOrder = failoverOrder;
        }

        public Builder deadline(Duration deadline) { this.deadline = deadline; return this; }
        public Builder acquireTimeout(Duration acquireTimeout) { this.acquireTimeout = acquireTimeout; return this; }
        public Builder maxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; return this; }
        public Builder baseBackoff(Duration baseBackoff) { this.baseBackoff = baseBackoff; return this; }
        public Builder maxConcurrency(int maxConcurrency) { this.maxConcurrency = maxConcurrency; return this; }

        public DeliveryPolicy build() {
            return new DeliveryPolicy(mode, failoverOrder, deadline, acquireTimeout,
                    maxAttempts, baseBackoff, maxConcurrency);
        }
    }

    public enum Mode {
        FAN_OUT,
        FAILOVER
    }
}
```

语义说明：

- `mode`
  - 通过 `fanOut()` / `failover(order)` 返回的 builder 链式构造，命名参数避免 `Duration`/`int` 位置传错；不建议直接调用 canonical 构造器。
  - `FAN_OUT`：所有可投递渠道都尝试发送。
  - `FAILOVER`：按 `failoverOrder` 给定的固定顺序逐个尝试渠道，首个成功即停止。
- `failoverOrder`
  - 仅在 `FAILOVER` 下生效。
  - 表示渠道优先级列表；运行时会与当前请求的目标渠道求交集。
  - 始终要求非 null，并在构造时做 `List.copyOf(...)`；其中元素也必须非 null。
  - 若目标渠道中的某个渠道未出现在 `failoverOrder` 中，则该渠道记为 `SkippedResult(NOT_IN_FAILOVER_ORDER)`，而不是被静默忽略。
  - 若 `mode == FAILOVER`，则 `failoverOrder` 不能为空，且不能重复或引用未注册渠道。
  - `fanOut(...)` 工厂不接收 `failoverOrder`；内部以空列表占位，不参与执行。
- `deadline`
  - 单次 `broadcast()` 的总超时预算。
- `acquireTimeout`
  - 单个投递任务等待全局并发许可的最长时间。
  - 实际生效等待窗口为 `min(acquireTimeout, remainingDeadline)`。
  - 到时仍拿不到许可，才返回 `RejectedResult`。
- `maxAttempts`
  - 每个"收件人-渠道"组合的最大尝试次数，含首次调用。
- `baseBackoff`
  - 固定指数退避基线。
  - 第 1 次重试前等待 `baseBackoff`，第 2 次重试前等待 `baseBackoff * 2`，第 3 次重试前等待 `baseBackoff * 4`，以此类推。
  - 本版不引入 jitter，也不额外配置最大退避上限；真正等待时长始终取"理论退避值"和"剩余 deadline"中的较小者。
- `maxConcurrency`
  - 当前引擎实例的全局并发上限。

> 首版不再提供"按渠道并发上限""按租户限流""按失败率熔断"这些框架能力。若真实业务证明这些能力必要，再作为后续版本增量引入。

### 2.3 投递结果

```java
/**
 * 标准化投递结果。sealed interface + record 子类型：每个结果类型只携带它需要的字段，
 * 非法状态（如 SUCCESS 带 errorCode、SKIPPED 带 attempts）在类型层面就无法表达，
 * 无需运行时校验矩阵。
 */
public sealed interface DeliveryResult
        permits SuccessResult, SkippedResult, FailedResult, TimeoutResult, RejectedResult {

    String notificationId();
    String recipientId();
    ChannelType channel();

    default boolean isSuccess() { return this instanceof SuccessResult; }
    default boolean isSkipped() { return this instanceof SkippedResult; }
    /** 未成功且未跳过：覆盖 FailedResult / TimeoutResult / RejectedResult。 */
    default boolean isFailure() { return !isSuccess() && !isSkipped(); }

    static String requireId(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(name + " must not have surrounding whitespace");
        }
        return value;
    }

    static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }

    static String requireNullOrNonBlank(String value, String name) {
        if (value != null && value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

/** 渠道受理成功。attempts >= 1；providerMessageId 可为 null（上游未返回可记录 ID）。 */
public record SuccessResult(
        String notificationId, String recipientId, ChannelType channel,
        String providerMessageId, int attempts, Duration elapsed) implements DeliveryResult {

    public SuccessResult {
        notificationId = requireId(notificationId, "notificationId");
        recipientId = requireId(recipientId, "recipientId");
        channel = requireNonNull(channel, "channel");
        elapsed = requireNonNull(elapsed, "elapsed");
        if (attempts < 1) {
            throw new IllegalArgumentException("attempts must be >= 1 for SUCCESS");
        }
        providerMessageId = requireNullOrNonBlank(providerMessageId, "providerMessageId");
    }
}

/** 跳过投递，未调用 send(...)。 */
public record SkippedResult(
        String notificationId, String recipientId, ChannelType channel,
        SkipReason reason) implements DeliveryResult {

    public SkippedResult {
        notificationId = requireId(notificationId, "notificationId");
        recipientId = requireId(recipientId, "recipientId");
        channel = requireNonNull(channel, "channel");
        reason = requireNonNull(reason, "reason");
    }
}

public enum SkipReason {
    NO_ADDRESS, UNSUPPORTED, NOT_IN_FAILOVER_ORDER, FAILOVER_SHORT_CIRCUITED
}

/** 投递失败。errorCode / errorMessage 独立可选，不要求成对。 */
public record FailedResult(
        String notificationId, String recipientId, ChannelType channel,
        FailReason reason, int attempts, Duration elapsed,
        String errorCode, String errorMessage) implements DeliveryResult {

    public FailedResult {
        notificationId = requireId(notificationId, "notificationId");
        recipientId = requireId(recipientId, "recipientId");
        channel = requireNonNull(channel, "channel");
        reason = requireNonNull(reason, "reason");
        elapsed = requireNonNull(elapsed, "elapsed");
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts must not be negative");
        }
        if (reason == FailReason.NON_RETRYABLE && attempts < 1) {
            throw new IllegalArgumentException("NON_RETRYABLE requires attempts >= 1");
        }
        if (reason == FailReason.CHANNEL_UNAVAILABLE && attempts != 0) {
            throw new IllegalArgumentException("CHANNEL_UNAVAILABLE requires attempts == 0");
        }
        errorCode = requireNullOrNonBlank(errorCode, "errorCode");
        errorMessage = requireNullOrNonBlank(errorMessage, "errorMessage");
    }
}

public enum FailReason {
    NON_RETRYABLE, EXCEPTION, CHANNEL_UNAVAILABLE
}

/** deadline 耗尽。attempts 可为 0（首次发送前就超时）或 >= 1。 */
public record TimeoutResult(
        String notificationId, String recipientId, ChannelType channel,
        int attempts, Duration elapsed) implements DeliveryResult {

    public TimeoutResult {
        notificationId = requireId(notificationId, "notificationId");
        recipientId = requireId(recipientId, "recipientId");
        channel = requireNonNull(channel, "channel");
        elapsed = requireNonNull(elapsed, "elapsed");
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts must not be negative");
        }
    }
}

/** 并发许可等待超时后被拒绝。未调用 send(...)。 */
public record RejectedResult(
        String notificationId, String recipientId, ChannelType channel) implements DeliveryResult {

    public RejectedResult {
        notificationId = requireId(notificationId, "notificationId");
        recipientId = requireId(recipientId, "recipientId");
        channel = requireNonNull(channel, "channel");
    }
}
```

设计要点：

- **sealed interface 保证合法状态**：`providerMessageId` 只存在于 `SuccessResult`，`errorCode/errorMessage` 只存在于 `FailedResult`，`attempts` 不存在于 `SkippedResult/RejectedResult`。无需运行时校验矩阵，非法组合在类型层面就无法表达。
- **公开结果不暴露 `Throwable`**：结果需稳定序列化和记录；完整堆栈放日志或内部诊断事件。
- `attempts` 表示**实际调用 `NotificationChannel.send(...)` 的次数**：
  - `SuccessResult`：>= 1
  - `FailedResult(NON_RETRYABLE)`：>= 1（`NonRetryableException` 由 `send(...)` 抛出）
  - `FailedResult(EXCEPTION)`：>= 0（可能发生在 `supports(...)` 或发送期间）
  - `FailedResult(CHANNEL_UNAVAILABLE)`：== 0（渠道未注册）
  - `TimeoutResult`：>= 0（deadline 可能在首次发送前或发送后耗尽）
  - `SkippedResult` / `RejectedResult`：无此字段（未调用 `send(...)`）
- 由引擎自身 deadline 驱动的 `future.cancel(true)` / 线程中断，最终结果一律收口为 `TimeoutResult`，而不是 `FailedResult(EXCEPTION)`。
- `SkippedResult(FAILOVER_SHORT_CIRCUITED)` 表示该渠道原本属于本次 failover 候选集，但在它之前已有渠道成功，因此被明确短路而不是"结果缺失"。
- `errorCode` 与 `errorMessage` **不要求成对出现**；有就填，没有就留 `null`，不要为了对称性伪造字段。
- 对于只依赖结果类型就能完全解释的结果，例如 `SkippedResult(NO_ADDRESS)`、`RejectedResult`、`FailedResult(CHANNEL_UNAVAILABLE)`，调用方不应期待 `errorCode/errorMessage` 一定存在。

### 2.4 聚合结果

```java
public record BroadcastResult(String notificationId, List<DeliveryResult> results, Duration elapsed) {
    public BroadcastResult {
        if (notificationId == null || notificationId.isBlank()) {
            throw new IllegalArgumentException("notificationId must not be blank");
        }
        if (elapsed == null) {
            throw new IllegalArgumentException("elapsed must not be null");
        }
        results = List.copyOf(results);
        if (results.stream().anyMatch(r -> !notificationId.equals(r.notificationId()))) {
            throw new IllegalArgumentException("broadcast result notificationId must match every delivery result");
        }
    }

    public long successCount() {
        return results.stream().filter(DeliveryResult::isSuccess).count();
    }

    public long skippedCount() {
        return results.stream().filter(DeliveryResult::isSkipped).count();
    }

    public long attemptedCount() {
        return results.size() - skippedCount();
    }

    public List<DeliveryResult> failures() {
        return results.stream().filter(DeliveryResult::isFailure).toList();
    }
}
```

语义约定：

- `elapsed` 是整次 `broadcast()` 从开始到返回的总耗时（区别于 `DeliveryResult.elapsed` 的单 recipient-channel 任务级耗时）。
- `SKIPPED` 不计入 `attempted`；`attempted = results.size() - skippedCount`
- 对于每个 recipient，在"目标渠道集合"确定之后，该 recipient 下的每个 target channel 都必须恰好对应一条 `DeliveryResult`；引擎不能让某个目标渠道静默消失
- `results()` 的顺序必须是**确定性的**，不能依赖并发完成先后：先按输入 `recipient` 顺序分组，再按 `channel.name()` 字典序输出
- `failures()` 保持 `results()` 中的相对顺序，不再单独重排
- `BroadcastResult.notificationId()` 必须与 `results()` 中每条 `DeliveryResult.notificationId()` 完全一致
- 引擎**不提供** `fullyDelivered()` 之类的聚合布尔值，因为"算不算完整送达"取决于业务语义：
  - 有的业务只要求"至少一个渠道成功"
  - 有的业务要求"所有目标渠道都完成尝试且无失败"
  - 有的业务把 `SkippedResult(NO_ADDRESS)` 视为数据质量问题，而不是技术成功

---

## 3. 核心扩展点

首版只保留一个必须扩展点。

### 3.1 NotificationChannel

```java
public interface NotificationChannel {
    ChannelType type();

    /**
     * 返回 false 表示该通知对当前收件人不支持，记为 `SkippedResult(UNSUPPORTED)`。
     * 该方法应是快速、局部、无副作用的能力判断；不应真正发送通知，也不应做远程探测。
     * 若抛异常，引擎记为 `FailedResult(EXCEPTION)`（`attempts` 可为 0）。
     * 地址可由渠道从 r.addresses().get(type()) 取得（调用时一定非空白）。
     */
    default boolean supports(Notification n, Recipient r) {
        return true;
    }

    /**
     * 真正的发送动作。地址由渠道从 r.addresses().get(type()) 自取；引擎在调用前已完成 NO_ADDRESS
     * 判定，保证该地址存在且非空白，渠道无需再判空。
     * 成功返回 providerMessageId；若上游未提供可记录 ID，可返回 null；抛异常表示失败。
     * 返回值只能是 null 或非空白字符串（空白串视为非法，引擎记 FailedResult(EXCEPTION)）。
     * 渠道实现必须响应线程中断；若捕获 InterruptedException，应恢复中断标记并尽快退出，不能吞掉中断后继续阻塞。
     * 若中断是由引擎在 deadline 到达后触发，最终结果由引擎统一收口为 `TimeoutResult`。
     * 不声明 checked 异常：渠道应将 checked 异常包装为 RuntimeException 抛出；除 NonRetryableException
     * 外，所有异常引擎都会按策略重试。
     */
    String send(Notification n, Recipient r);
}

public class NonRetryableException extends RuntimeException {
    public NonRetryableException(String message) { super(message); }
    public NonRetryableException(String message, Throwable cause) { super(message, cause); }
}
```

渠道初始化规则：

- 渠道实例由宿主应用显式创建，不由引擎自动发现、自动装配或按需懒创建。
- 渠道依赖的外部资源也由宿主应用提前初始化，例如 SMTP client、短信网关 SDK、HTTP client、认证凭证。
- `registerChannel(...)` 注册的是**已经完成初始化**的渠道实例；首版假定这些实例按 engine 级别长期复用，而不是"每次 `broadcast()` 新建一次"。
- 引擎内部只维护 `ChannelType -> NotificationChannel` 的注册表，不承担工厂、容器或插件管理器职责。
- `type()` 必须稳定、非 null、可重复调用且结果一致；引擎不会接受可变渠道标识。
- 渠道实现必须是**线程安全或并发可复用**的，因为同一个渠道实例可能被多个 recipient 并发调用。
- 若某个渠道适配器天然不是线程安全的，应由该适配器在内部自行串行化，而不是要求引擎做按渠道加锁。

生命周期约定：

- `NotificationEngine.close()` 只负责释放引擎自身持有的资源，例如由引擎自己创建且 `ownsExecutor == true` 的执行器。
- 引擎默认**不负责关闭渠道底层资源**；渠道生命周期仍由宿主应用管理。
- 若某个渠道实现内部持有可关闭资源，宿主应用应在引擎外统一管理其关闭时机，而不是依赖引擎回调。

---

## 4. 执行模型

### 4.1 引擎接口

```java
public interface NotificationEngine extends AutoCloseable {
    static Builder builder() {
        return new Builder();
    }

    /**
     * n and recipients must not be null; recipients must not contain null elements.
     * An empty recipients list returns an empty BroadcastResult bound to n.notificationId().
     */
    BroadcastResult broadcast(Notification n, List<Recipient> recipients);
}
```

首版只保留同步接口。异步调用由调用方自行包裹线程池、消息队列或 actor。
引擎的**唯一标准化输出**是 `BroadcastResult`；本版不再提供 attempt 级 observer SPI。

输入顺序合同：

- `broadcast()` 接收 `List<Recipient>`，而不是无序 `Collection`。
- `broadcast()` 的两个参数 `Notification n` 与 `List<Recipient> recipients` 都必须非 null；`recipients` 中的元素也必须非 null。
- `recipients` 允许为空；此时返回的 `BroadcastResult` 必须保留 `notificationId == n.notificationId()`，且 `results()` / `failures()` / 各类计数均为空或 0。
- v1 保留调用方给出的 recipient 顺序，因为它同时决定 `BroadcastResult.results()` 的主排序键。
- `recipientId` 在单次广播内必须唯一；调用方可直接用 `recipientId` 把结果映射回收件人。

关闭语义：

- `close()` 必须幂等。
- `close()` 之后再次调用 `broadcast()`，应直接抛出 `IllegalStateException`。
- `close()` 对已经开始的 `broadcast()` 采用**优雅关闭**语义：不再接受新调用，但允许在途任务继续运行到各自的 `deadline` 或自然结束。
- 若引擎持有自己的执行器且 `ownsExecutor == true`，`close()` 调用 `shutdown()`，而不是 `shutdownNow()`。

### 4.2 固定执行流程

对每个 recipient，执行如下固定流程：

1. 计算目标渠道集合
   - `Notification.targetChannels` 为空：使用当前已注册的全部渠道
   - `Notification.targetChannels` 非空：仅使用调用方指定的渠道

2. 对每个目标渠道做分类
   - 未注册：`FailedResult(CHANNEL_UNAVAILABLE)`
   - 无地址：`SkippedResult(NO_ADDRESS)`；判定条件为缺 key、空串或全空白
   - `supports(...) == false`：`SkippedResult(UNSUPPORTED)`
   - 其余进入真正投递阶段

3. 按 `DeliveryPolicy.mode` 执行
   - `FAN_OUT`：所有可投递渠道并发尝试
   - `FAILOVER`：按 `DeliveryPolicy.failoverOrder` 中的固定顺序，对"已经通过前一阶段分类且出现在 `failoverOrder` 中的可投递渠道"串行尝试，首个成功即停
   - 对属于目标渠道、但未出现在 `failoverOrder` 中的渠道，补记 `SkippedResult(NOT_IN_FAILOVER_ORDER)`
   - 对属于 failover 候选集、但排在首个成功渠道之后而未再尝试的渠道，补记 `SkippedResult(FAILOVER_SHORT_CIRCUITED)`
   - 若 `FAILOVER` 过滤后没有任何渠道可尝试，本次 recipient 的全部结果只会来自前一阶段的分类结果与上述补记结果

4. 对每个真正投递的任务执行固定重试
   - 最多 `maxAttempts`
   - 固定指数退避：第 `k` 次重试前等待 `baseBackoff * 2^(k-1)`
   - 若下一次退避或发送前 `deadline` 已耗尽，则直接返回 `TimeoutResult`
   - 若任务已开始执行，而 deadline 在等待发送结果期间耗尽，引擎会取消该任务并最终返回 `TimeoutResult`
   - 单次 `broadcast` 共享总 `deadline`
   - `NonRetryableException` 不重试

5. 汇总 `DeliveryResult`，返回 `BroadcastResult`
   - `broadcast()` 永不因单个渠道失败而抛出异常
   - 仅在参数明显非法或引擎自身处于不可用状态时允许直接抛异常

### 4.3 超时与并发

- 内部使用 `ExecutorService`
- 默认 `newVirtualThreadPerTaskExecutor()`
- 并发控制只有一层：全局 `maxConcurrency`
  - 引擎内部维护一个全局 `Semaphore(maxConcurrency)`
  - 每个真正投递任务在发送前执行 `tryAcquire(min(acquireTimeout, remainingDeadline))`
  - 若剩余 `deadline` 已耗尽，则直接返回 `TimeoutResult`，不再等待并发许可
  - 只有在有效等待窗口耗尽后仍拿不到许可时，才返回 `RejectedResult`
  - 获取成功：发送结束后 `release()`
- 超时控制基于显式 `Future`
  - `future.get(remaining, MILLISECONDS)`
  - 超时则 `future.cancel(true)`
  - deadline 驱动的 `future.cancel(true)` 最终结果统一映射为 `TimeoutResult`
  - 渠道实现必须响应中断

> 本版明确不再内建"按渠道舱壁""熔断器""限流器"。如果调用方需要这些能力，应在引擎外层组合现有基础设施或未来版本再评估引入。  
> 本版的并发控制语义是"有界等待后拒绝"，而不是"瞬时争抢失败即拒绝"。

---

## 5. 装配

```java
public final class Builder {
    public Builder executor(ExecutorService executor);
    public Builder ownsExecutor(boolean owns);
    public Builder policy(DeliveryPolicy policy);
    public Builder registerChannel(NotificationChannel c);
    public NotificationEngine build();
}
```

装配约定：

- `Builder` 是启动期装配器，而不是运行期并发配置面；首版不要求它线程安全。
- `policy(...)` 是必填项；`policy(null)` 应立即抛 `IllegalArgumentException`。
- `registerChannel(...)` 只接受非 null 渠道实例；`c.type()` 也必须非 null，否则应立即抛 `IllegalArgumentException`。
- `registerChannel(...)` 接收的是宿主应用已经初始化完成的渠道实例。
- 渠道注册通常发生在应用启动阶段，而不是每次发送前动态增删。
- 若未显式调用 `executor(...)`，`build()` 默认创建一个 `newVirtualThreadPerTaskExecutor()`，并视为 `ownsExecutor == true`。
- 若显式调用了 `executor(executor)` 但未调用 `ownsExecutor(...)`，默认视为 `ownsExecutor == false`，即引擎默认不关闭外部传入的执行器。
- 若调用方同时传入外部执行器并显式设置 `ownsExecutor(true)`，则表示调用方把该执行器的关闭责任转交给引擎；`close()` 时会对它执行 `shutdown()`。
- 引擎对外不暴露"按名称查找渠道实例"的公共 API；注册完成后，仅由引擎内部在执行时按 `ChannelType` 查找。
- 若调用方需要日志、指标或 tracing，应包装 `NotificationChannel` 或在 `BroadcastResult` 返回后采集，而不是依赖引擎内置 observer。

首版不再提供：

- `discoverChannels(...)`
- `routingStrategy(...)`
- `rateLimiter(...)`
- `retryPolicy(...)`
- `circuitBreaker(...)`
- `rateLimitKey(...)`

### 5.1 `build()` 自检

`build()` 失败即抛异常：

- 未设置 `policy`
- 未注册任何渠道
- 出现重复的 `ChannelType`
- `failoverOrder` 含重复渠道
- `failoverOrder` 含未注册渠道

仅依赖 `DeliveryPolicy` 自身即可判定的非法值，例如 `null` 字段、负时长、非正并发/重试次数、`FAILOVER` 空顺序，应在 `DeliveryPolicy` 构造时立即失败，而不是等到 `build()`。  
仅依赖单个 builder setter 即可判定的非法值，例如 `policy(null)`、`executor(null)`、`registerChannel(null)`、`registerChannel(c)` 且 `c.type() == null`，也应在对应 setter 调用时立即失败。

---

## 6. 最小示例

```java
NotificationChannel email = new NotificationChannel() {
    public ChannelType type() { return ChannelType.EMAIL; }

    public String send(Notification n, Recipient r) {
        String addr = r.addresses().get(type());
        System.out.printf("[EMAIL->%s] %s%n", addr, n.payload().get("text"));
        return addr;
    }
};

try (NotificationEngine engine = NotificationEngine.builder()
        .policy(DeliveryPolicy.fanOut()
                .deadline(Duration.ofSeconds(10))
                .acquireTimeout(Duration.ofMillis(500))
                .maxAttempts(3)
                .baseBackoff(Duration.ofMillis(200))
                .maxConcurrency(256)
                .build())
        .registerChannel(email)
        .build()) {

    Notification n = new Notification(
            "ntf-1001",
            Map.of("text", "order paid"),
            Set.of(ChannelType.EMAIL));

    List<Recipient> recipients = List.of(
            new Recipient("u1", Map.of(ChannelType.EMAIL, "u1@x.com"), Map.of()),
            new Recipient("u2", Map.of(ChannelType.EMAIL, "u2@x.com"), Map.of()));

    BroadcastResult result = engine.broadcast(n, recipients);
    System.out.println(result.successCount());
}
```

---

## 7. 能力边界与非目标

| 边界 | 说明 | 用户应对 |
|---|---|---|
| 至多一次 + 崩溃即丢 | 在途任务和重试全在内存，进程崩溃即丢。 | 需要 at-least-once 时，由外层接 outbox/MQ。 |
| 不提供跨调用幂等 | v1 不负责去重，也不引入持久化状态。 | 需要幂等时，由外层 outbox / 去重表 / MQ 消费语义承担；`notificationId` 是天然的外层幂等键。 |
| 不内建熔断/限流 | 首版不重造 resilience 框架。 | 由调用方在外层组合现有基础设施。 |
| 不做插件发现 | 首版没有真实插件生态。 | 显式注册渠道实现。 |
| 不负责收件人解析 | 引擎不查人、不查角色、不查租户。 | 调用方提前准备好 `Recipient.addresses`。 |
| 地址模型只支持单字符串 | v1 不支持"同一渠道多个地址"或结构化地址对象。 | 调用方先把复杂目标解析为单一字符串地址。 |
| `FAILOVER` 顺序固定但需显式提供 | 首版不猜测渠道优先级。 | 使用 `FAILOVER` 时显式提供合法的 `failoverOrder`；未列入顺序表的目标渠道会被标记为 `SKIPPED`。 |
| 并发拒绝是"等待超时后拒绝" | 首版不采用瞬时争抢失败即丢弃。 | 若希望更激进或更宽松，应在外层包装不同调度策略。 |
| 渠道实例按 engine 级别复用 | 引擎不为每次发送新建渠道实例。 | 渠道实现应保证线程安全或内部串行化。 |
| `ChannelType` 不是自由字符串 | 它直接参与判等、注册和排序。 | 统一使用规范化后的大写命名，如 `EMAIL`、`PUSH`、`DINGTALK_BOT`。 |
| `notificationId` 与 `recipientId` 不是同级 ID | 前者是通知标识，后者只是收件人业务标签。 | 不要把 `recipientId` 当作单次广播内的唯一键。 |
| 地址格式不是引擎级合同 | 引擎只判断"有没有地址"，不判断"地址是否合法"。 | 邮箱、手机号、URL 等格式校验由具体渠道实现负责。 |
| 不提供 observer SPI | 首版不标准化 attempt 级观测事件。 | 需要日志、指标或 tracing 时，在引擎外包装渠道或消费 `BroadcastResult`。 |
| `SUCCESS` 不等于最终送达 | 仅表示渠道受理。 | 送达回执由业务方自行处理。 |
| 公开结果不暴露 `Throwable` | 结果需稳定序列化和记录。 | 完整堆栈放日志或内部诊断事件。 |

原则：**先证明需要，再开放抽象。**

---

## 8. v1 公共接口摘要

下表是当前文档收敛后的 **v1 直接实现面**。如果某个能力不在这里，默认就不属于首版公共契约。

| 契约 | 作用 | v1 约束 |
|---|---|---|
| `Notification(notificationId, payload, targetChannels)` | 表示一条待发送通知 | `notificationId` 必须非空且无首尾空白；`payload` 只做透传 |
| `Recipient(recipientId, addresses, context)` | 表示一个收件人及其可用地址 | `recipientId` 必须非空、无首尾空白且单次广播内唯一；每渠道最多一个字符串地址；`context` 只做透传 |
| `ChannelType(name)` | 表示一个渠道类型标识符 | `name` 会被规范化为大写；只允许 `[A-Z][A-Z0-9_]*` |
| `DeliveryPolicy(...)` | 固定投递策略 | 通过 `fanOut()` / `failover(order)` builder 链式构造；退避、超时、并发语义已写死 |
| `Builder` | 装配引擎实例 | `policy(...)` 必填；默认自建虚拟线程执行器且默认自有；外部执行器默认不由引擎关闭 |
| `NotificationChannel` | 唯一必须扩展点 | 渠道实例按 engine 级复用；`type()` 必须稳定；`supports()` 只做快速能力判断；`send()` 不声明 checked 异常、必须响应中断；地址由渠道从 `r.addresses()` 自取 |
| `NotificationEngine.broadcast(...)` | 同步执行一次广播 | 签名为 `broadcast(Notification, List<Recipient>)`；保留调用方输入顺序 |
| `BroadcastResult` | 标准化输出 | 提供结果列表、计数、失败列表与总耗时 `elapsed`；不提供业务化布尔结论 |

刻意不进入 v1 公共接口的能力：

- observer / event bus
- 自动发现 / 插件装配
- 路由 SPI
- 幂等 / 去重
- 持久化 / 崩溃恢复
- 分布式限流 / 熔断 / 舱壁

补充合同：

- `payload`、`targetChannels`、`addresses`、`context` 都必须非 null，并在构造时做容器层面的防御性复制；容器中的 key / element / value 也必须非 null；"未指定目标渠道"只允许用空 `Set` 表达。
- `payload/context` 只复制容器，不深拷贝内部对象；若 value object 可变，调用方必须自行保证其在一次 `broadcast()` 生命周期内不被修改。
- `BroadcastResult.results()` 的输出顺序固定为"先按输入 recipient 顺序，再按 `channel.name()` 字典序"，不能依赖并发执行顺序。
- `broadcast()` 要求 `n/recipients` 非 null，且 `recipients` 中不能含 null 元素；空 recipient 列表返回与输入 `notificationId` 绑定的空结果。
- `recipients` 中的 `recipientId` 必须在单次广播内唯一；重复时 `broadcast()` 抛 `IllegalArgumentException`。
- `NO_ADDRESS` 的判定固定为：缺 key、空串或全空白地址。
- `attempts` 表示真实 `send(...)` 调用次数；`SkippedResult/RejectedResult` 无此字段，`SuccessResult/FailedResult(NON_RETRYABLE)` >= 1，`FailedResult(EXCEPTION)` >= 0，`FailedResult(CHANNEL_UNAVAILABLE)` == 0，`TimeoutResult` >= 0。
- `FAILOVER` 下首个成功之后的后续候选渠道不会静默消失，必须补记为 `SkippedResult(FAILOVER_SHORT_CIRCUITED)`。
- `BroadcastResult.notificationId()` 必须与内部每条 `DeliveryResult.notificationId()` 一致。
- 对于每个 recipient 和每个 target channel，`BroadcastResult.results()` 都必须恰好有一条最终结果。
- `providerMessageId` 只存在于 `SuccessResult`；其值可以为 `null`，表示渠道受理成功但上游没有返回可记录消息 ID。
- `errorCode/errorMessage` 是独立可选字段，不要求成对出现。
- `DeliveryResult` 为 sealed interface + record 子类型；合法状态由类型系统保证，无需 private 构造器或工厂方法收口。
- deadline 驱动的取消/中断最终统一映射为 `TimeoutResult`，不再额外分叉成 `FailedResult(EXCEPTION)`。
- `Builder` 的 setter 对显而易见的非法输入立即失败；`executor(...)` 缺省时自建并自有执行器，显式传入外部执行器时默认不接管关闭责任。

---

## 9. 实现补充约定（落地前补全）

> 以下约定在 v22 契约基础上补全实现盲点，不改变已有契约。

### 9.1 并发模型

实现时必须遵守三层并发：

- **recipient 之间**：并发。多个 recipient 的投递任务并发提交到执行器，受全局 `maxConcurrency` 限制。
- **recipient 内**：取决于 `DeliveryPolicy.mode`。
  - `FAN_OUT`：该 recipient 的所有可投递渠道**并发**尝试，每个渠道各占一个并发许可。
  - `FAILOVER`：候选渠道按 `failoverOrder` **串行**尝试，同一时刻只占一个并发许可；首个成功即短路。
- **许可粒度**：`maxConcurrency` 限制的是**真正投递任务（`send(...)` 调用）数**，不是 recipient 数。
  - `FAN_OUT` 下一个 recipient 有 3 个可投递渠道，最多同时占 3 个许可。
  - `FAILOVER` 下同一 recipient 同时最多占 1 个许可。

### 9.2 `broadcast()` 线程安全

- 同一个 `NotificationEngine` 实例**允许被多线程并发调用 `broadcast(...)`**。
- 引擎内部的注册表、`Semaphore`、`closed` 标志必须线程安全；`broadcast(...)` 与 `close()` 可并发，`close()` 之后的新 `broadcast(...)` 抛 `IllegalStateException`。
- 渠道实例按 engine 级复用且要求并发可复用（见 §3），因此并发 `broadcast(...)` 会并发调用同一渠道实例的 `send(...)`。

### 9.3 send 异常诊断与字段填充

- 引擎捕获 `NotificationChannel.send(...)` 抛出的异常时，**必须至少 `log.warn` 一次并包含完整堆栈**；`DeliveryResult` 只保留 `errorCode/errorMessage` 文本，不暴露 `Throwable`。这样即使调用方未包装渠道，异常堆栈也不会丢失。
- `errorCode` 默认填 `ex.getClass().getName()`，`errorMessage` 默认填 `ex.getMessage()`（可为 `null`）；二者独立可选，不要求成对。
- `send(...)` 返回空白字符串（非 null 但全空白）视为非法：引擎记 `FailedResult(EXCEPTION)`，不当成功。

### 9.4 被中断的 send 与 `attempts`

- deadline 在 `send(...)` 已开始执行后耗尽、引擎 `future.cancel(true)` 中断该任务：若 `send(...)` 已被实际调用（即便被中断），计入 `attempts + 1`，最终结果仍为 `TimeoutResult`。
- deadline 在 `send(...)` 提交前耗尽：`attempts = 0`，结果为 `TimeoutResult`。

### 9.5 `elapsed` 语义

- `SuccessResult/FailedResult/TimeoutResult` 的 `elapsed` 是**该 recipient-channel 投递任务从首次进入投递阶段到结束的总耗时**，含重试与退避等待，不是单次 `send(...)` 耗时。
