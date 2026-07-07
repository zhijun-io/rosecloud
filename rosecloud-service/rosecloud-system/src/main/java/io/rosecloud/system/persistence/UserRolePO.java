package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * MyBatis-Plus persistent object for the {@code sys_user_role} link table.
 * Standalone (no audit base) since it is a pure join table.
 */
@TableName("sys_user_role")
public class UserRolePO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long roleId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
}
