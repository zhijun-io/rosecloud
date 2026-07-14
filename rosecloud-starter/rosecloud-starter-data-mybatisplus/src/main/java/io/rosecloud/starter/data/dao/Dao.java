package io.rosecloud.starter.data.dao;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Generic data access object interface.
 *
 * <p>Wraps the persistence layer behind a clean contract, decoupling services from
 * direct mapper calls. Mirrors ThingsBoard's {@code Dao<T>} pattern.
 *
 * @param <T> the domain type
 * @param <K> the ID type ({@link String} or {@link Long})
 */
public interface Dao<T, K extends Serializable> {

    /**
     * Find by primary key.
     *
     * @return the domain object, or {@link Optional#empty()} if not found
     */
    Optional<T> findById(K id);

    /**
     * Persist the domain object.
     *
     * <p>If the ID is {@code null}, an INSERT is performed and the returned domain
     * carries the generated ID. Otherwise an UPDATE is performed.
     *
     * @return the domain object with any generated state populated
     */
    T save(T domain);

    /**
     * Delete by primary key.
     */
    void removeById(K id);

    /**
     * Check existence by primary key.
     */
    boolean existsById(K id);

    /**
     * Return all rows converted to the domain type.
     */
    List<T> findAll();

    /**
     * Total row count.
     */
    long count();
}
