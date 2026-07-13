package io.rosecloud.common.core.model;

/**
 * Inverse of {@link ToData}: copies a framework-free domain object {@code D} onto this
 * persistent entity and returns it for fluent use. Mirrors the write direction that
 * ThingsBoard leaves to per-service construction; here each paged entity owns both
 * conversions so the mapping lives beside the data, not in the service layer.
 *
 * @param <D> the domain (record/DTO) type
 * @param <E> the entity type implementing this interface
 */
public interface ToEntity<D, E> {
    E toEntity(D data);
}
