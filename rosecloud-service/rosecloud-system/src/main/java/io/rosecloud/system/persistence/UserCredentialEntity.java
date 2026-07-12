package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.rosecloud.starter.data.BaseEntity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent entity for {@code user_credential}; confined to infrastructure. */
@TableName("user_credential")
@Getter
@Setter
@NoArgsConstructor
public class UserCredentialEntity extends BaseEntity {

    private Long userId;
    private String password;
    private LocalDateTime passwordChangedTime;
    private String activateToken;
    private LocalDateTime expireTime;
    private LocalDateTime usedTime;
    private LocalDateTime sendTime;
    private LocalDateTime lastLoginTime;
    @Version
    private Long version;
}
