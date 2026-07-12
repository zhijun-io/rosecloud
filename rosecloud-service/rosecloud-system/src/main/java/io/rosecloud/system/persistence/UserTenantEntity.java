package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent entity for {@code sys_user_tenant}; confined to infrastructure. */
@TableName("sys_user_tenant")
@Getter
@Setter
@NoArgsConstructor
public class UserTenantEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String tenantId;
    private Integer isPrimary;
}
