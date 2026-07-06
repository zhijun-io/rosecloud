package io.rosecloud.auth.controller;

import io.rosecloud.auth.service.AuthService;
import io.rosecloud.auth.service.dto.LoginRequest;
import io.rosecloud.auth.service.dto.RefreshRequest;
import io.rosecloud.auth.service.dto.TokenResponse;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody LoginRequest request, HttpServletRequest http) {
        return ApiResponse.ok(authService.login(request, clientIp(http), http.getHeader(HttpHeaders.USER_AGENT)));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
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
