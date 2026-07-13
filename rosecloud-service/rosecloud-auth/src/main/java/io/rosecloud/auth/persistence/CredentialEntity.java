package io.rosecloud.auth.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent entity for {@code auth_credential}; confined to infrastructure. */
@TableName("auth_credential")
@Getter
@Setter
@NoArgsConstructor
public class CredentialEntity extends BaseEntity {

    private Long userId;
    private String passwordHash;
    private LocalDateTime passwordChangedTime;
    private Integer authStatus;
    private LocalDateTime lastLoginTime;
}
