package io.rosecloud.common.security.model;

import java.time.Instant;

public record LoginSession(String id, String token, String refreshToken, Long userId, String username, String nickname,
                            String clientIp, String userAgent, Instant loginAt, Instant expireAt, String deviceId) {

    public LoginSession(String id, String token, String refreshToken, Long userId, String username, String nickname,
                        String clientIp, String userAgent, Instant loginAt, Instant expireAt) {
        this(id, token, refreshToken, userId, username, nickname, clientIp, userAgent, loginAt, expireAt, null);
    }
}
