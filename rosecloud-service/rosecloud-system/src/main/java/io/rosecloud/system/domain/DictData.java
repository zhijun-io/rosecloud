package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasStatus;

import java.util.Objects;

/** Domain view of a dictionary item. ORM-free; mapped to/from {@code sys_dict_data}. */
public final class DictData implements HasId, HasStatus<Integer> {

    private final Long id;
    private final String dictCode;
    private final String label;
    private final String value;
    private final Integer sort;
    private final Integer status;
    private final String remark;

    public DictData(Long id, String dictCode, String label, String value, Integer sort, Integer status, String remark) {
        this.id = id;
        this.dictCode = dictCode;
        this.label = label;
        this.value = value;
        this.sort = sort;
        this.status = status;
        this.remark = remark;
    }

    public Long getId() { return id; }
    public String getDictCode() { return dictCode; }
    public String getLabel() { return label; }
    public String getValue() { return value; }
    public Integer getSort() { return sort; }
    public Integer getStatus() { return status; }
    public String getRemark() { return remark; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DictData that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(dictCode, that.dictCode) && Objects.equals(label, that.label)
                && Objects.equals(value, that.value) && Objects.equals(sort, that.sort)
                && Objects.equals(status, that.status) && Objects.equals(remark, that.remark);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dictCode, label, value, sort, status, remark);
    }

    @Override
    public String toString() {
        return "DictData[" +
                "id=" + id +
                ", dictCode=" + dictCode +
                ", label=" + label +
                ", value=" + value +
                ", sort=" + sort +
                ", status=" + status +
                ", remark=" + remark +
                ']';
    }
}
