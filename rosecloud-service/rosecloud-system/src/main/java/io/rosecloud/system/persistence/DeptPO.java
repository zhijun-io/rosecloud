package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

/** MyBatis-Plus persistent object for {@code sys_dept}; confined to infrastructure. */
@TableName("sys_dept")
public class DeptPO extends BaseEntity {

    private Long parentId;
    private String name;
    private Integer sort;
    private Integer status;
    private String leader;
    private String phone;

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getLeader() { return leader; }
    public void setLeader(String leader) { this.leader = leader; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
