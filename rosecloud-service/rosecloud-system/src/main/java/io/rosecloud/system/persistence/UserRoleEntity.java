package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MyBatis-Plus persistent entity for the {@code sys_user_role} link table.
 * Standalone (no audit base) since it is a pure join table.
 */
@TableName("sys_user_role")
@Getter
@Setter
@NoArgsConstructor
public class UserRoleEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long roleId;
}
