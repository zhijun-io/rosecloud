package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.common.core.model.ToData;
import io.rosecloud.starter.data.BaseEntity;
import io.rosecloud.system.domain.Role;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent object for {@code sys_role}; confined to infrastructure. */
@Getter
@Setter
@NoArgsConstructor
@TableName("sys_role")
public class RoleEntity extends BaseEntity implements ToData<Role> {

    private String code;
    private String name;

    @Override
    public Role toData() {
        return new Role(getId(), code, name, getCreateTime(), getCreateBy(), getUpdateTime(), getUpdateBy());
    }
}
