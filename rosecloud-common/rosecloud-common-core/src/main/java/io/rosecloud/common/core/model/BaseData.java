package io.rosecloud.common.core.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Base data container with common creation metadata.
 */
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

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(createdTime);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BaseData other = (BaseData) obj;
        return createdTime == other.createdTime;
    }

    @Override
    public String toString() {
        return "BaseData[createdTime=" + createdTime + "]";
    }
}
