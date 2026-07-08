package io.rosecloud.auth.controller;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.starter.security.jwt.SessionInvalidationEvent;
import io.rosecloud.starter.security.jwt.SessionInvalidationPublisher;
import io.rosecloud.starter.security.jwt.TokenRevocationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Internal endpoint for the system service to revoke a token. Not gateway-routed. */
@RestController
@RequestMapping("/internal/revoke")
public class InternalRevocationController {

    private final TokenRevocationService tokenRevocationService;
    private final SessionInvalidationPublisher sessionInvalidationPublisher;

    public InternalRevocationController(TokenRevocationService tokenRevocationService,
                                   SessionInvalidationPublisher sessionInvalidationPublisher) {
        this.tokenRevocationService = tokenRevocationService;
        this.sessionInvalidationPublisher = sessionInvalidationPublisher;
    }

    @PostMapping
    public ApiResponse<Void> revoke(@RequestParam("jti") String jti,
                                    @RequestParam(value = "expireTime", required = false) LocalDateTime expireTime) {
        Instant expiresAt = expireTime == null ? null : expireTime.atZone(ZoneId.systemDefault()).toInstant();
        tokenRevocationService.revoke(jti, expiresAt);
        sessionInvalidationPublisher.publish(SessionInvalidationEvent.forJti(jti, expiresAt));
        return ApiResponse.ok();
    }
}
