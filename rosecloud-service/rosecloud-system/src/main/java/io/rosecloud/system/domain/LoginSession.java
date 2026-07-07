package io.rosecloud.system.domain;

import java.time.LocalDateTime;

/** Domain view of a login session. ORM-free; mapped to/from {@code sys_login_session}. */
public record LoginSession(Long id, String jti, Long userId, String username, Long tenantId,
                           LocalDateTime loginTime, LocalDateTime expireTime, String ip,
                           String userAgent, Integer status) {
}
