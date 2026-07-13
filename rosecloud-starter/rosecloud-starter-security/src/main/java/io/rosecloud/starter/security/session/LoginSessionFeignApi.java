package io.rosecloud.starter.security.session;

import io.rosecloud.common.security.model.LoginSession;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for {@link LoginSessionApi}. Only internal platform services (system, and any
 * service that needs to verify or propagate revocation) call this; it must never be exposed to
 * external clients. The {@code @InternalApi} side of these endpoints requires the shared
 * {@code X-Internal} token, so callers must configure {@code rosecloud.security.internal-token}.
 */
@FeignClient(name = "rosecloud-auth", contextId = "loginSessionApi", path = "/api/auth/sessions")
public interface LoginSessionFeignApi extends LoginSessionApi {

    @Override
    @PostMapping("/internal")
    void save(LoginSession session);

    @Override
    @GetMapping("/internal/revoked")
    boolean isRevoked(@RequestParam("token") String token);

    @Override
    @PostMapping("/internal/revoke")
    void revoke(@RequestBody String token);

    @Override
    @PostMapping("/internal/revoke/session/{sessionId}")
    void revokeBySessionId(@PathVariable("sessionId") String sessionId);

    @Override
    @PostMapping("/internal/revoke/user/{userId}")
    void revokeByUserId(@PathVariable("userId") Long userId);
}
