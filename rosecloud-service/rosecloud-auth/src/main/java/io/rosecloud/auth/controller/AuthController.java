package io.rosecloud.auth.controller;

import io.rosecloud.auth.service.AuthService;
import io.rosecloud.auth.service.dto.LoginRequest;
import io.rosecloud.auth.service.dto.RefreshRequest;
import io.rosecloud.auth.service.dto.TokenResponse;
import io.rosecloud.auth.support.ClientIpResolver;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.ServiceMetadata;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/auth")
public class AuthController {

    private final AuthService authService;
    private final ClientIpResolver clientIpResolver;

    public AuthController(AuthService authService, ClientIpResolver clientIpResolver) {
        this.authService = authService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody LoginRequest request, HttpServletRequest http) {
        return ApiResponse.ok(authService.login(request, clientIpResolver.resolve(http), http.getHeader(HttpHeaders.USER_AGENT)));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        authService.logout(auth);
        return ApiResponse.ok();
    }
}
