package io.rosecloud.common.core.event;

/**
 * 实体变更类型。
 * 发布到 Spring ApplicationEvent 的 {@link EntityChangedEvent} 挂载该类型，供监听器判断变更性质。
 */
public enum EntityChangeType {

    CREATED,
    UPDATED,
    DELETED

}
