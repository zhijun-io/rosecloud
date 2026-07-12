package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent object for {@code sys_login_log}; confined to infrastructure. */
@TableName("sys_login_log")
@Getter
@Setter
@NoArgsConstructor
public class LoginLogEntity extends BaseEntity {

    private String username;
    private Integer success;
    private String failReason;
    private String ip;
    private String userAgent;
    private LocalDateTime loginTime;
}
