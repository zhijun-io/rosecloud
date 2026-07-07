package io.rosecloud.auth.controller;

import io.rosecloud.api.session.RevokeRequest;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.starter.security.jwt.TokenRevocationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;

/** Internal endpoint for the system service to revoke a token. Not gateway-routed. */
@RestController
@RequestMapping("/internal/revoke")
public class InternalRevocationController {

    private final TokenRevocationService tokenRevocationService;

    public InternalRevocationController(TokenRevocationService tokenRevocationService) {
        this.tokenRevocationService = tokenRevocationService;
    }

    @PostMapping
    public ApiResponse<Void> revoke(@RequestBody RevokeRequest request) {
        Instant expiresAt = request.expireTime() == null
                ? null
                : request.expireTime().atZone(ZoneId.systemDefault()).toInstant();
        tokenRevocationService.revoke(request.jti(), expiresAt);
        return ApiResponse.ok();
    }
}
