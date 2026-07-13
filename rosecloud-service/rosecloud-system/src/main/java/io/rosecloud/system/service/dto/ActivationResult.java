package io.rosecloud.system.service.dto;

/**
 * Result returned after a successful user activation, containing the JWT token pair
 * and the access token expiration in seconds so the client can schedule a refresh.
 */
public record ActivationResult(
        String token,
        String refreshToken,
        long expiresIn
) {
}
