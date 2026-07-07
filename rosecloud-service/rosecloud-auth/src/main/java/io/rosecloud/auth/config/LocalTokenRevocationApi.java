package io.rosecloud.auth.config;

import io.rosecloud.api.session.RevokeRequest;
import io.rosecloud.api.session.TokenRevocationApi;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.starter.security.jwt.TokenRevocationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;

/**
 * In-process {@link TokenRevocationApi} for monolith mode: delegates to the
 * shared {@link TokenRevocationService} instead of Feign.
 */
@Component
@ConditionalOnProperty(prefix = "spring.application", name = "name", havingValue = "rosecloud-monolith")
public class LocalTokenRevocationApi implements TokenRevocationApi {

    private final TokenRevocationService tokenRevocationService;

    public LocalTokenRevocationApi(TokenRevocationService tokenRevocationService) {
        this.tokenRevocationService = tokenRevocationService;
    }

    @Override
    public ApiResponse<Void> revoke(RevokeRequest request) {
        Instant expiresAt = request.expireTime() == null
                ? null
                : request.expireTime().atZone(ZoneId.systemDefault()).toInstant();
        tokenRevocationService.revoke(request.jti(), expiresAt);
        return ApiResponse.ok();
    }
}
