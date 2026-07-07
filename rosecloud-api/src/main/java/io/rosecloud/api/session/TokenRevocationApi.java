package io.rosecloud.api.session;

import io.rosecloud.common.core.model.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign contract for the system service to revoke a token (force-logout) via the
 * auth service, which owns the {@code TokenRevocationService}. The gateway /
 * monolith filter then rejects the revoked {@code jti}.
 */
@FeignClient(name = "rosecloud-auth", contextId = "tokenRevocationApi", path = "/internal/revoke")
public interface TokenRevocationApi {

    @PostMapping
    ApiResponse<Void> revoke(@RequestBody RevokeRequest request);
}
