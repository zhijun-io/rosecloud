package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent entity for {@code sys_user}; confined to infrastructure. */
@TableName("sys_user")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity extends BaseEntity {

    private String nickname;
    private Integer status;
    private String tenantId;
    private String email;
    private String phone;
    private String additionalInfo;
}
