package io.rosecloud.system.controller;

import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.system.service.dto.ActivationConfirmRequest;
import io.rosecloud.system.service.dto.ActivationResendRequest;
import io.rosecloud.system.service.dto.UserActivationInfo;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.common.security.model.LoginSession;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.session.SessionStore;
import io.rosecloud.common.security.token.JwtPair;
import io.rosecloud.common.security.token.TokenFactory;
import io.rosecloud.system.service.UserActivationService;
import io.rosecloud.system.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/noauth")
public class NoAuthController {

    private final UserActivationService userActivationService;
    private final UserService userService;
    private final TokenFactory tokenFactory;
    private final SessionStore sessionStore;
    private final LoginLogApi loginLogApi;

    public NoAuthController(UserActivationService userActivationService,
                            UserService userService,
                            TokenFactory tokenFactory,
                            SessionStore sessionStore,
                            LoginLogApi loginLogApi) {
        this.userActivationService = userActivationService;
        this.userService = userService;
        this.tokenFactory = tokenFactory;
        this.sessionStore = sessionStore;
        this.loginLogApi = loginLogApi;
    }

    @GetMapping("/activate")
    public ApiResponse<UserActivationInfo> check(@RequestParam("activateToken") String activateToken) {
        return ApiResponse.ok(userActivationService.check(activateToken));
    }

    @PostMapping("/activate")
    public ApiResponse<LoginTokenPairResponse> activate(@RequestBody ActivationConfirmRequest request,
                                                        HttpServletRequest http) {
        UserActivationInfo info = userActivationService.confirm(request.activateToken(), request.password());
        SecurityUser securityUser = userService.loadByUsername(info.username());
        JwtPair tokenPair = tokenFactory.createTokenPair(securityUser);

        String ip = resolveIp(http);
        String userAgent = http.getHeader(HttpHeaders.USER_AGENT);
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(tokenFactory.getAccessTokenExpirationSeconds());
        sessionStore.save(new LoginSession(
                UUID.randomUUID().toString(),
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                securityUser.getUserId(),
                securityUser.getUsername(),
                securityUser.getNickname(),
                ip,
                truncate(userAgent, 512),
                now,
                expireAt));
        loginLogApi.record(new LoginLogRequest(securityUser.getUsername(), true, null, ip, truncate(userAgent, 512)));

        LoginTokenPairResponse tokenResponse = new LoginTokenPairResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenFactory.getAccessTokenExpirationSeconds());

        return ApiResponse.ok(tokenResponse);
    }

    @PostMapping("/activate/resend")
    public ApiResponse<UserActivationInfo> resend(@RequestBody ActivationResendRequest request) {
        return ApiResponse.ok(userActivationService.resend(request.username()));
    }

    private static String resolveIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String truncate(String value, int max) {
        return value != null && value.length() > max ? value.substring(0, max) : value;
    }

    private record LoginTokenPairResponse(String token, String refreshToken, long expiresIn) {
    }
}
