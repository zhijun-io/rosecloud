package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent object for {@code sys_role}; confined to infrastructure. */
@Getter
@Setter
@NoArgsConstructor
@TableName("sys_role")
public class RoleEntity extends BaseEntity {

    private String code;
    private String name;
}
