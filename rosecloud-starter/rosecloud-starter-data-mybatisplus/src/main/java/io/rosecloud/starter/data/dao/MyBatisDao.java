package io.rosecloud.starter.data.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.SortField;
import io.rosecloud.common.core.model.ToData;
import io.rosecloud.starter.data.PagedResults;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Base DAO implementation backed by MyBatis-Plus {@link BaseMapper}.
 *
 * <p>Subclasses provide the entity-to-domain conversion logic via abstract
 * methods. The {@link #save(Object)} method transparently handles INSERT (null
 * ID) vs UPDATE (non-null ID).
 *
 * <p>Mirrors ThingsBoard's pattern where every entity has a dedicated DAO
 * that wraps persistence calls.
 *
 * @param <T> the domain type
 * @param <K> the ID type ({@link String} or {@link Long})
 * @param <E> the MyBatis-Plus entity type; must implement {@link ToData}{@code <T>}
 */
public abstract class MyBatisDao<T, K extends Serializable, E extends ToData<T>>
        implements Dao<T, K> {

    protected final BaseMapper<E> mapper;
    private final Class<E> entityClass;

    /**
     * @param mapper      the MyBatis-Plus mapper for the entity
     * @param entityClass the concrete entity class (needed for paging metadata)
     */
    protected MyBatisDao(BaseMapper<E> mapper, Class<E> entityClass) {
        this.mapper = mapper;
        this.entityClass = entityClass;
    }

    @Override
    public Optional<T> findById(K id) {
        return Optional.ofNullable(mapper.selectById(id)).map(E::toData);
    }

    @Override
    public T save(T domain) {
        boolean isNew = getId(domain) == null;
        E entity = toEntity(domain);
        if (isNew) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return toDomain(entity);
    }

    @Override
    public void removeById(K id) {
        mapper.deleteById(id);
    }

    @Override
    public boolean existsById(K id) {
        return mapper.exists(new QueryWrapper<E>().eq("id", id));
    }

    @Override
    public List<T> findAll() {
        return mapper.selectList(null).stream().map(E::toData).toList();
    }

    @Override
    public long count() {
        return mapper.selectCount(null);
    }

    /**
     * Execute a paged query.
     *
     * @param query        the paging / sorting request
     * @param filter       builds a {@link LambdaQueryWrapper} from the query
     * @param defaultSorts fallback sort fields when the request has none
     * @param <Q>          type of the page query
     * @return paged results of domain objects
     */
    public <Q extends PageQuery> PagedData<T> page(Q query,
            Function<Q, LambdaQueryWrapper<E>> filter,
            SortField... defaultSorts) {
        return PagedResults.page(query, entityClass, mapper, filter, defaultSorts);
    }

    /**
     * Extract the ID from a domain object. Used by {@link #save(Object)} to
     * decide INSERT vs UPDATE.
     */
    protected abstract K getId(T domain);

    /**
     * Convert a domain object to its MyBatis-Plus entity counterpart.
     */
    protected abstract E toEntity(T domain);

    /**
     * Convert an entity back to a domain object.
     *
     * <p>Defaults to {@link ToData#toData()}. Override if post-processing is
     * needed (e.g. after INSERT, the entity carries a generated ID that must be
     * reflected in the returned domain via alternate construction).
     */
    protected T toDomain(E entity) {
        return entity.toData();
    }
}
