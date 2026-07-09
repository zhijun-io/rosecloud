package io.rosecloud.common.security.model;

import java.time.Instant;

public record LoginSession(String id, String token, Long userId, String username, String nickname,
                            String clientIp, String userAgent, Instant loginAt, Instant expireAt) {
}
