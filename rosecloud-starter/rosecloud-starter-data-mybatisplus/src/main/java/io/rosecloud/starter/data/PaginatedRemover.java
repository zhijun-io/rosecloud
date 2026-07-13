package io.rosecloud.starter.data;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 批量实体清理器，借鉴 ThingsBoard {@code PaginatedRemover} 模式。
 *
 * <p>以固定大小的分页（默认每页 100 条）遍历并删除实体，避免单条大批量
 * DELETE 带来的长事务和锁竞争。适用于租户级联删除等场景。
 *
 * <p>用法：
 * <pre>{@code
 * PaginatedRemover<String, UserEntity> userRemover = PaginatedRemover.of(
 *     tenantId -> userMapper.selectList(
 *         new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getTenantId, tenantId)
 *             .last("LIMIT 100")),
 *     userMapper::deleteById
 * );
 * userRemover.removeEntities(tenantId);
 * }</pre>
 *
 * @param <I> 查询参数（如 tenantId）
 * @param <E> 实体类型
 */
public final class PaginatedRemover<I, E> {

    private static final int DEFAULT_LIMIT = 100;

    private final int pageSize;
    private final Function<I, List<E>> finder;
    private final Consumer<E> remover;

    private PaginatedRemover(int pageSize, Function<I, List<E>> finder,
                             Consumer<E> remover) {
        this.pageSize = pageSize;
        this.finder = finder;
        this.remover = remover;
    }

    /**
     * 创建分页删除器，默认每页 100 条。
     *
     * @param finder  返回指定参数下的实体列表（已分页）
     * @param remover 删除单个实体的回调
     */
    public static <I, E> PaginatedRemover<I, E> of(Function<I, List<E>> finder,
                                                    Consumer<E> remover) {
        return new PaginatedRemover<>(DEFAULT_LIMIT, finder, remover);
    }

    /**
     * 创建分页删除器，自定义每页大小。
     */
    public static <I, E> PaginatedRemover<I, E> of(int pageSize,
                                                    Function<I, List<E>> finder,
                                                    Consumer<E> remover) {
        return new PaginatedRemover<>(pageSize, finder, remover);
    }

    /**
     * 遍历删除所有匹配的实体。
     */
    public void removeEntities(I id) {
        boolean hasNext = true;
        while (hasNext) {
            List<E> batch = finder.apply(id);
            if (batch == null || batch.isEmpty()) {
                return;
            }
            for (E entity : batch) {
                remover.accept(entity);
            }
            hasNext = batch.size() >= pageSize;
        }
    }
}
