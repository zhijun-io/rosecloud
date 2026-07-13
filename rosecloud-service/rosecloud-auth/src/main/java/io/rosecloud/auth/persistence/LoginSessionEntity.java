package io.rosecloud.auth.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent object for {@code auth_login_session}; confined to infrastructure. */
@TableName("auth_login_session")
@Getter
@Setter
@NoArgsConstructor
public class LoginSessionEntity extends BaseEntity {

    private String sessionId;
    private Long userId;
    private String username;
    private String nickname;
    private String token;
    private String refreshToken;
    private String clientIp;
    private String userAgent;
    private String deviceId;
    private LocalDateTime loginAt;
    private LocalDateTime expireAt;
    private Integer revoked;
}
