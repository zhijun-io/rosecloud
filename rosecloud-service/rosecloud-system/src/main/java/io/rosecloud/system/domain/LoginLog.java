package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.HasId;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Value;

/** Domain view of a login-audit entry. ORM-free; mapped to/from {@code sys_login_log}. */
@Value
@AllArgsConstructor
public final class LoginLog implements HasId {

    Long id;
    String username;
    boolean success;
    String failReason;
    String ip;
    String userAgent;
    LocalDateTime loginTime;
}
