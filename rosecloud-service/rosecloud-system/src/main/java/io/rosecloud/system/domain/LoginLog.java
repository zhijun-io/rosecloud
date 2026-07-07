package io.rosecloud.system.domain;

import java.time.LocalDateTime;

/** Domain view of a login-audit entry. ORM-free; mapped to/from {@code sys_login_log}. */
public record LoginLog(Long id, String username, boolean success, String failReason,
                       String ip, String userAgent, LocalDateTime loginTime) {
}
