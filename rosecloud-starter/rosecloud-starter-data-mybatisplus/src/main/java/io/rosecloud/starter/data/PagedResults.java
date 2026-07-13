package io.rosecloud.starter.data;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.SortColumnMapper;
import io.rosecloud.common.core.model.SortDirection;
import io.rosecloud.common.core.model.SortField;
import io.rosecloud.common.core.model.ToData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Bridges the framework-free {@link PageQuery} to MyBatis-Plus and assembles the
 * {@link PagedData} envelope. Every paged query shares one code path (replaces the per-service
 * {@code new PageImpl<>(records, PageRequest.of(...), total)} boilerplate). The sortable
 * columns are taken from the entity's MyBatis-Plus metadata, so there is no hand-maintained
 * per-entity column map; the max page size is enforced by the pagination interceptor.
 */
public final class PagedResults {

    private PagedResults() {
    }

    /**
     * Execute a paged query in one step: build the MyBatis-Plus page, apply {@code filter}
     * (keyword/time-window derived from the request), fetch, map records, and wrap into a
     * {@link PagedData}. Sortable columns come from {@code entityClass}'s metadata; {@code defaultSorts}
     * are appended when the request does not supply an explicit sort.
     */
    public static <Q extends PageQuery, E extends ToData<T>, T> PagedData<T> page(Q query,
            Class<E> entityClass, BaseMapper<E> baseMapper,
            Function<Q, LambdaQueryWrapper<E>> filter, SortField... defaultSorts) {
        Page<E> page = toPage(query, entityClass, defaultSorts);
        LambdaQueryWrapper<E> wrapper = filter.apply(query);
        IPage<E> result = baseMapper.selectPage(page, wrapper);
        List<T> records = result.getRecords().stream().map(ToData::toData).toList();
        return of(records, result.getTotal(), query);
    }

    /**
     * Variant of {@link #page(PageQuery, Class, BaseMapper, Function, SortField...)} that maps each
     * record with an explicit {@code mapper} instead of the entity's {@link ToData} contract.
     */
    public static <Q extends PageQuery, E, T> PagedData<T> page(Q query,
            Class<E> entityClass, BaseMapper<E> baseMapper,
            Function<Q, LambdaQueryWrapper<E>> filter, Function<E, T> mapper, SortField... defaultSorts) {
        Page<E> page = toPage(query, entityClass, defaultSorts);
        LambdaQueryWrapper<E> wrapper = filter.apply(query);
        IPage<E> result = baseMapper.selectPage(page, wrapper);
        List<T> records = result.getRecords().stream().map(mapper).toList();
        return of(records, result.getTotal(), query);
    }

    /**
     * Build a 1-based MyBatis-Plus page (size clamped to a minimum of 1) whose orders are the
     * requested sort properties (validated against {@code entityClass}'s columns) followed by
     * {@code defaultSorts}. Unknown sort properties are rejected by {@link SortColumnMapper}.
     */
    public static <T> Page<T> toPage(PageQuery query, Class<T> entityClass, SortField... defaultSorts) {
        return toPage(query, sortMapper(entityClass, defaultSorts));
    }

    private static <T> Page<T> toPage(PageQuery query, SortColumnMapper mapper) {
        int size = Math.max(1, query.getSize());
        int page = Math.max(1, query.getPage());
        Page<T> mpPage = new Page<>(page, size);
        for (SortField field : mapper.resolve(query.getSorts())) {
            String column = mapper.toColumn(field.property());
            mpPage.addOrder(field.direction() == SortDirection.DESC
                    ? OrderItem.desc(column)
                    : OrderItem.asc(column));
        }
        return mpPage;
    }

    /**
     * Sortable columns are every persistent field of {@code entityClass} (key + fields), mapped
     * property name &rarr; column by MyBatis-Plus metadata, with {@code defaultSorts} as fallback.
     */
    private static SortColumnMapper sortMapper(Class<?> entityClass, SortField... defaultSorts) {
        TableInfo info = TableInfoHelper.getTableInfo(entityClass);
        Map<String, String> columns = new HashMap<>();
        if (info.getKeyProperty() != null) {
            columns.put(info.getKeyProperty(), info.getKeyColumn());
        }
        for (TableFieldInfo field : info.getFieldList()) {
            columns.put(field.getProperty(), field.getColumn());
        }
        return new SortColumnMapper(columns, defaultSorts);
    }

    /**
     * Wrap already-mapped records into a {@link PagedData}. {@code hasNext} is derived from
     * the requested page vs. the computed total page count.
     */
    public static <T> PagedData<T> of(List<T> records, long total, PageQuery query) {
        int size = Math.max(1, query.getSize());
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
        boolean hasNext = query.getPage() < totalPages;
        return new PagedData<>(records, totalPages, total, hasNext);
    }

    /**
     * Slice an already-materialized in-memory list into a {@link PagedData}. Used where the
     * data source is not paged at the store level (e.g. session listings).
     */
    public static <T> PagedData<T> slice(List<T> all, PageQuery query) {
        int size = Math.max(1, query.getSize());
        int page = Math.max(1, query.getPage());
        int from = Math.min((page - 1) * size, all.size());
        int to = Math.min(from + size, all.size());
        return of(all.subList(from, to), all.size(), query);
    }
}
