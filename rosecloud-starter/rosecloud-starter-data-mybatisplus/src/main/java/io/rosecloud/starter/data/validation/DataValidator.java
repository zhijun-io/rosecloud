package io.rosecloud.starter.data.validation;

import io.rosecloud.common.core.error.BizException;

import java.util.Optional;

/**
 * Base class for domain validation logic.
 *
 * <p>Mirrors ThingsBoard's {@code DataValidator} pattern. Each CRUD operation
 * has a lifecycle hook that subclasses override to enforce business rules.
 * Hooks throw {@link BizException} when a constraint is violated.
 *
 * <p>Services call the validation hooks before delegating to a {@code Dao}:
 * <pre>{@code
 *     validator.validateCreate(data);
 *     dao.save(data);
 * }</pre>
 *
 * @param <D> the domain type
 * @param <K> the ID type ({@link String} or {@link Long})
 */
public abstract class DataValidator<D, K> {

    /**
     * Validate before creating an entity.
     *
     * @throws BizException if a constraint is violated
     */
    public void validateCreate(D data) {
        // no-op by default
    }

    /**
     * Validate before updating an entity.
     *
     * @param data the updated domain object
     * @param old  the previously persisted domain object, or empty if not found
     * @throws BizException if a constraint is violated
     */
    public void validateUpdate(D data, Optional<D> old) {
        // no-op by default
    }

    /**
     * Validate before deleting an entity.
     *
     * @param data the domain object to delete
     * @throws BizException if a constraint is violated
     */
    public void validateDelete(D data) {
        // no-op by default
    }

    /**
     * Extract the ID from a domain object.
     */
    protected abstract K getId(D data);
}
