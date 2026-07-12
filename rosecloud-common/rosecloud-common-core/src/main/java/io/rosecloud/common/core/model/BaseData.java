package io.rosecloud.common.core.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Base data container with common creation metadata.
 *
 * <p>Deliberately declares no {@code equals/hashCode}: comparing only the single
 * {@link #createdTime} field (the only state here) would wrongly treat unrelated
 * instances as equal. Concrete subclasses define their own equality semantics, and
 * {@link BaseDataWithAdditionalInfo} relies on {@code callSuper=true} (identity for
 * this base). Falls back to Object identity, which is the safe default.
 */
@Getter
@Setter
@ToString
public abstract class BaseData {

    private LocalDateTime createdTime;

    protected BaseData() {
    }

    protected BaseData(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    protected BaseData(BaseData data) {
        this.createdTime = data.createdTime;
    }
}
