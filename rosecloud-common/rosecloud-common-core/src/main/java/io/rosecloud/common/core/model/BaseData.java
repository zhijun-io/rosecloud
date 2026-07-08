package io.rosecloud.common.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

/** Base data container with common creation metadata. */
public abstract class BaseData {

    public static final ObjectMapper mapper = new ObjectMapper();

    private long createdTime;

    protected BaseData() {
    }

    protected BaseData(long createdTime) {
        this.createdTime = createdTime;
    }

    protected BaseData(BaseData data) {
        this.createdTime = data.createdTime;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
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
