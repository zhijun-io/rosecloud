package io.rosecloud.common.core.event;

/**
 * 实体变更事件。
 * 作为纯数据载体（POJO）跨模块传播，不依赖 Spring ApplicationEvent，
 * Shipped from the starter 层使用 Spring ApplicationEventPublisher 包装后发布。
 *
 * @param <T> 变更前后的实体类型
 */
public record EntityChangedEvent<T>(
        String entityType,
        Object entityId,
        EntityChangeType changeType,
        String tenantId,
        T before,
        T after
) {

    public EntityChangedEvent {
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException("entityType must not be blank");
        }
        if (entityId == null) {
            throw new IllegalArgumentException("entityId must not be null");
        }
        if (changeType == null) {
            throw new IllegalArgumentException("changeType must not be null");
        }
    }

    /** 便捷工厂：创建实体 */
    public static <T> EntityChangedEvent<T> created(String entityType, Object entityId, String tenantId, T after) {
        return new EntityChangedEvent<>(entityType, entityId, EntityChangeType.CREATED, tenantId, null, after);
    }

    /** 便捷工厂：更新实体 */
    public static <T> EntityChangedEvent<T> updated(String entityType, Object entityId, String tenantId, T before, T after) {
        return new EntityChangedEvent<>(entityType, entityId, EntityChangeType.UPDATED, tenantId, before, after);
    }

    /** 便捷工厂：删除实体 */
    public static <T> EntityChangedEvent<T> deleted(String entityType, Object entityId, String tenantId, T before) {
        return new EntityChangedEvent<>(entityType, entityId, EntityChangeType.DELETED, tenantId, before, null);
    }
}
