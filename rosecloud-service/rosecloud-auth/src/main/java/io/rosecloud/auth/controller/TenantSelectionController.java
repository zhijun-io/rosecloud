package io.rosecloud.auth.controller;

import io.rosecloud.auth.service.TenantSelectionService;
import io.rosecloud.auth.service.dto.TenantSelectionResponse;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.token.JwtPair;
import io.rosecloud.starter.security.token.BearerTokenExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/auth/tenants")
public class TenantSelectionController {

    private final TenantSelectionService tenantSelectionService;
    private final BearerTokenExtractor tokenExtractor = new BearerTokenExtractor();

    public TenantSelectionController(TenantSelectionService tenantSelectionService) {
        this.tenantSelectionService = tenantSelectionService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ApiResponse<TenantSelectionResponse> current(Authentication authentication) {
        return ApiResponse.ok(tenantSelectionService.getSelection(currentUser(authentication)));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{tenantId}/switch")
    public ApiResponse<JwtPair> switchTenant(@PathVariable String tenantId,
                                             Authentication authentication,
                                             HttpServletRequest request) {
        SecurityUser securityUser = currentUser(authentication);
        String currentToken = tokenExtractor.extract(request);
        String clientIp = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        return ApiResponse.ok(tenantSelectionService.switchTenant(
                securityUser, currentToken, tenantId, clientIp, userAgent));
    }

    private static SecurityUser currentUser(Authentication authentication) {
        return (SecurityUser) authentication.getPrincipal();
    }
}
