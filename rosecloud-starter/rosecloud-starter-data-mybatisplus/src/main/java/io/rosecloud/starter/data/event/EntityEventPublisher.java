package io.rosecloud.starter.data.event;

import io.rosecloud.common.core.event.EntityChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 实体变更事件发布器。
 *
 * <p>借鉴 ThingsBoard {@code AbstractCachedEntityService.publishEvictEvent()} 模式：
 * <ul>
 *   <li>有事务时：通过 Spring {@code ApplicationEventPublisher} 发布事件，
 *       由 {@code CacheEvictionListener} 的 {@code @TransactionalEventListener(AFTER_COMMIT)} 在事务提交后失效缓存</li>
 *   <li>无事务时：直接调用 {@link CacheEvictionListener#evictNow(EntityChangedEvent)} 即时失效</li>
 * </ul>
 */
public class EntityEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EntityEventPublisher.class);

    private final ApplicationEventPublisher publisher;
    private CacheEvictionListener cacheEvictionListener;

    public EntityEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /** 设置非事务环境下的直接失效回调。由 auto-configuration 注入。 */
    public void setCacheEvictionListener(CacheEvictionListener listener) {
        this.cacheEvictionListener = listener;
    }

    /**
     * 发布实体变更事件。
     *
     * <p>有事务时走 Spring Event 机制（事务提交后生效），
     * 无事务时直接执行缓存失效（即时生效）。
     */
    public void publish(EntityChangedEvent<?> event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            publisher.publishEvent(event);
        } else {
            log.debug("No active transaction — evicting cache [{}] immediately for key [{}]",
                    event.entityType(), event.entityId());
            if (cacheEvictionListener != null) {
                cacheEvictionListener.evictNow(event);
            } else {
                publisher.publishEvent(event);
            }
        }
    }

    /** 便捷方法：发布创建事件。 */
    public <T> void created(String entityType, Object entityId, String tenantId, T after) {
        publish(EntityChangedEvent.created(entityType, entityId, tenantId, after));
    }

    /** 便捷方法：发布更新事件。 */
    public <T> void updated(String entityType, Object entityId, String tenantId, T before, T after) {
        publish(EntityChangedEvent.updated(entityType, entityId, tenantId, before, after));
    }

    /** 便捷方法：发布删除事件。 */
    public <T> void deleted(String entityType, Object entityId, String tenantId, T before) {
        publish(EntityChangedEvent.deleted(entityType, entityId, tenantId, before));
    }
}
