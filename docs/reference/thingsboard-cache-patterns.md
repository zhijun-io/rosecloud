# ThingsBoard 缓存模式参考

本文档记录 ThingsBoard 中值得 RoseCloud 借鉴的缓存相关模式，供后续优化参考。
相关源码路径：`/Users/zhijunio/github/thingsboard`

## 1. 事务安全的读穿透

### 问题
CaffeineEntityCache 的 `put()` 在事务提交前就写入缓存。
如果事务随后回滚，缓存中会残留脏数据。

### ThingsBoard 做法
`TbTransactionalCache.getAndPutInTransaction()` (common/cache/TbTransactionalCache.java)：
- 开启一个事务性缓存操作（`TbCacheTransaction`）
- 从数据库加载值后，通过 `cacheTransaction.put(key, value)` 暂存
- 事务提交时：调用 `cacheTransaction.commit()` 实际写入缓存
- 事务回滚时：调用 `cacheTransaction.rollback()`，缓存不变

### RoseCloud 适配建议
- 在 `EntityCache` 接口中新增 `getOrLoadTransactional(K key, Supplier<V> loader)` 默认方法
- 实现类 `CaffeineEntityCache` 利用 Spring 的 `TransactionSynchronizationManager.registerSynchronization()` 将缓存 put 注册为事务提交后的回调
- 也可以选择暂时不实现：当前场景下 Caffeine 5 分钟 TTL 会自然过期脏数据，且缓存写后的事务回滚概率低

## 2. 非事务环境下的即时缓存失效

### 问题
`CacheEvictionListener` 的 `@TransactionalEventListener(AFTER_COMMIT)` 仅在事务提交后执行失效。
如果代码在没有活跃事务的上下文中发布事件（如只读操作中手动 evict），事件永远不会被消费。

### ThingsBoard 做法
`AbstractCachedEntityService.publishEvictEvent()` (dao/entity/AbstractCachedEntityService.java)：
```java
if (TransactionSynchronizationManager.isActualTransactionActive()) {
    eventPublisher.publishEvent(event);
} else {
    handleEvictEvent(event);
}
```
有事务时走 event/`@TransactionalEventListener`，无事务时立即执行失效。

### RoseCloud 适配建议
- 在不改动 `CacheEvictionListener` 接口的前提下，新增一个 `EvictEventPublisher` 包装类
- 内部判断 `TransactionSynchronizationManager.isActualTransactionActive()`
- 有事务时：`publishEvent(event)`
- 无事务时：直接调用 `CacheEvictionListener.onEntityChanged(event)`
- 将服务类中的 `eventPublisher.publish(...)` 替换为 `evictEventPublisher.publish(...)`

## 3. PaginatedRemover — 批量实体清理

### 问题
删除租户等根实体时，需要级联删除大量关联实体（用户、设备、仪表盘等）。
直接 `delete where tenant_id = ?` 可能导致长事务或锁竞争。

### ThingsBoard 做法
`PaginatedRemover` (dao/service/PaginatedRemover.java)：
```java
public void removeEntities(TenantId tenantId, I id) {
    PageLink pageLink = new PageLink(DEFAULT_LIMIT);
    boolean hasNext = true;
    while (hasNext) {
        PageData<D> entities = findEntities(tenantId, id, pageLink);
        for (D entity : entities.getData()) {
            removeEntity(tenantId, entity);
        }
        hasNext = entities.hasNext();
    }
}
```
抽象方法 `findEntities()` 和 `removeEntity()` 由子类实现，每次仅处理 100 条。
配合 `@Transactional` 确保每批操作在独立事务中完成。

### RoseCloud 适配建议
- `TenantServiceImpl.deleteTenant()` 已包含约 10 个级联清理操作
- 当某个级联表数据量会超过几百条时（如 sys_user），改用 PaginatedRemover 模式分批删除
- 具体实现：在 system 模块新建一个 `UserPaginatedRemover` 继承 PaginatedRemover，调用 UserMapper 的分页查询方法

## 4. 缓存失效事件的数据载体设计对比

| 方面 | ThingsBoard | RoseCloud |
|------|-------------|-----------|
| 事件载体 | `DeleteEntityEvent` / `SaveEntityEvent` 等具体类型 | 泛型 `EntityChangedEvent<T>` |
| 事件类型枚举 | 通过具体事件类区分 | `EntityChangeType` 枚举 |
| 实体标识 | `EntityId` 对象（含 id + entityType） | `entityType + entityId` 两个字段 |
| 事务绑定 | `@TransactionalEventListener(AFTER_COMMIT)` | 同上 |
| 非事务 fallback | 有（publishEvictEvent 检查） | 无 |

RoseCloud 的泛型方案更简洁，但缺少非事务 fallback，可参考第 2 点改进。

## 5. 针对性缓存失效 vs 全缓存清除

### 问题
`UserServiceImpl.assignRoles()` 调用 `userSecurityCache.evictAll()` 清空所有用户的 SecurityUser 缓存，
导致所有用户在下次请求时都要重新 join 5 张表加载安全信息。在几百用户规模下引发 thundering herd。

### 修复
改为 `userSecurityCache.evict(user.getUsername())`，仅失效受影响的用户。
这不仅减少 DB 负载，还让其他用户的缓存命中率维持在 100%。

### 适用场景
- 任何只影响单个实体的变更（角色分配、菜单分配）都应针对性失效
- 仅在大型列表缓存（如 `menuListCache`、`dictDataByCodeCache`）使用 `evictAll()`

## 6. Caffeine 缓存统计监控

### Problem
Caffeine 默认启用 `recordStats()`，但没有暴露统计数据的入口。

### RoseCloud 实现
在 `EntityCache` 接口中新增 `default stats()` 方法，`CaffeineEntityCache` 返回实际统计数据。
新增 `CacheStatsController`（`/actuator/cache-stats`），收集所有 `EntityCache` 实例的
命中率、请求数、驱逐数、平均加载延迟等指标。

### ThingsBoard 参考
`TbCaffeineCacheConfiguration` 对所有 Cache 实例调用 `recordStats()`，
并通过 JMX 和自定义 monitor 端点暴露统计。

### 效果
```json
{
  "user.security": {
    "hitCount": 1234,
    "missCount": 56,
    "hitRate": 0.956,
    "evictionCount": 0,
    "averageLoadPenalty": 0.00234
  }
}
```
