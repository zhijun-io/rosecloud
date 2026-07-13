package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.common.core.model.ToData;
import io.rosecloud.common.core.util.Json;
import io.rosecloud.starter.data.BaseEntity;
import io.rosecloud.system.domain.User;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent entity for {@code sys_user}; confined to infrastructure. */
@TableName("sys_user")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity extends BaseEntity implements ToData<User> {

    private String nickname;
    private Integer status;
    private String tenantId;
    private String email;
    private String phone;
    private String additionalInfo;

    @Override
    public User toData() {
        return new User(getId(), loginName(), nickname, status, tenantId,
                Json.readTree(additionalInfo), getCreateTime(), getCreateBy(), getUpdateTime(), getUpdateBy());
    }

    private String loginName() {
        if (email != null && !email.isBlank()) {
            return email;
        }
        if (phone != null && !phone.isBlank()) {
            return phone;
        }
        return null;
    }
}
