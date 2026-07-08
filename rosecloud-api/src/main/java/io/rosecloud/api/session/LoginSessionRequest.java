package io.rosecloud.api.session;

import java.time.LocalDateTime;

/**
 * Login-session record reported by the auth service. Keyed by the access
 * token's {@code jti}; {@code expireTime} mirrors the token expiry so stale
 * sessions can be excluded from the online list.
 */
public record LoginSessionRequest(String jti, Long userId, String username, String tenantId,
                                  LocalDateTime expireTime, String ip, String userAgent) {
}
