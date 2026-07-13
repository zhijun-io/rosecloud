package io.rosecloud.common.core.model;

/**
 * Self-conversion contract: a persistence entity knows how to produce its ORM-free
 * domain object. Lets the pager build a {@link PagedData} without a separate mapper
 * function (mirrors ThingsBoard's {@code ToData}).
 *
 * @param <T> the domain type produced by {@link #toData()}
 */
public interface ToData<T> {

    T toData();
}
