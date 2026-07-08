package io.rosecloud.system.domain;

import java.util.Objects;

/** Domain view of a dictionary type. ORM-free; mapped to/from {@code sys_dict_type}. */
public final class DictType {

    private final Long id;
    private final String code;
    private final String name;
    private final Integer status;
    private final String remark;

    public DictType(Long id, String code, String name, Integer status, String remark) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.status = status;
        this.remark = remark;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Integer getStatus() { return status; }
    public String getRemark() { return remark; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DictType that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(code, that.code) && Objects.equals(name, that.name)
                && Objects.equals(status, that.status) && Objects.equals(remark, that.remark);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, name, status, remark);
    }

    @Override
    public String toString() {
        return "DictType[" +
                "id=" + id +
                ", code=" + code +
                ", name=" + name +
                ", status=" + status +
                ", remark=" + remark +
                ']';
    }
}
