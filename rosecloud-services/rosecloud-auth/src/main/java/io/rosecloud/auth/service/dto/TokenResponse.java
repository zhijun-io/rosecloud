package io.rosecloud.auth.service.dto;

public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {
}
