package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.HasCode;
import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasName;
import io.rosecloud.common.core.model.HasStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/** Domain view of a dictionary type. ORM-free; mapped to/from {@code sys_dict_type}. */
public final class DictType implements HasId, HasCode, HasName, HasStatus<Integer> {

    private final Long id;
    private final String code;
    private final String name;
    private final Integer status;
    private final String remark;
    private final LocalDateTime createTime;
    private final Long createBy;
    private final LocalDateTime updateTime;
    private final Long updateBy;

    public DictType(Long id, String code, String name, Integer status, String remark) {
        this(id, code, name, status, remark, null, null, null, null);
    }

    public DictType(Long id, String code, String name, Integer status, String remark,
                    LocalDateTime createTime, Long createBy, LocalDateTime updateTime, Long updateBy) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.status = status;
        this.remark = remark;
        this.createTime = createTime;
        this.createBy = createBy;
        this.updateTime = updateTime;
        this.updateBy = updateBy;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Integer getStatus() { return status; }
    public String getRemark() { return remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public Long getCreateBy() { return createBy; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public Long getUpdateBy() { return updateBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DictType that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(code, that.code) && Objects.equals(name, that.name)
                && Objects.equals(status, that.status) && Objects.equals(remark, that.remark)
                && Objects.equals(createTime, that.createTime) && Objects.equals(createBy, that.createBy)
                && Objects.equals(updateTime, that.updateTime) && Objects.equals(updateBy, that.updateBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, name, status, remark, createTime, createBy, updateTime, updateBy);
    }

    @Override
    public String toString() {
        return "DictType[" +
                "id=" + id +
                ", code=" + code +
                ", name=" + name +
                ", status=" + status +
                ", remark=" + remark +
                ", createTime=" + createTime +
                ", createBy=" + createBy +
                ", updateTime=" + updateTime +
                ", updateBy=" + updateBy +
                ']';
    }
}
