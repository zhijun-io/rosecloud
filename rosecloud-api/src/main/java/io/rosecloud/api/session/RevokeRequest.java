package io.rosecloud.api.session;

import java.time.LocalDateTime;

/** Revocation request reported by the system service to the auth service. */
public record RevokeRequest(String jti, LocalDateTime expireTime) {
}
