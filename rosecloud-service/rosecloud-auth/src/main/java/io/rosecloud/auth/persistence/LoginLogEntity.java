package io.rosecloud.auth.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.auth.domain.LoginLog;
import io.rosecloud.common.core.model.ToData;
import io.rosecloud.starter.data.BaseEntity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent object for {@code auth_login_log}; confined to infrastructure. */
@TableName("auth_login_log")
@Getter
@Setter
@NoArgsConstructor
public class LoginLogEntity extends BaseEntity implements ToData<LoginLog> {

    private String username;
    private Integer success;
    private String failReason;
    private String ip;
    private String userAgent;
    private String deviceId;
    private LocalDateTime loginTime;

    @Override
    public LoginLog toData() {
        return new LoginLog(getId(), username, success != null && success == 1, failReason, ip,
                userAgent, deviceId, loginTime);
    }
}
