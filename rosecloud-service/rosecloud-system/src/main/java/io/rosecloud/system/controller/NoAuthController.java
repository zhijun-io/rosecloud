package io.rosecloud.system.controller;

import io.rosecloud.api.user.ActivationConfirmRequest;
import io.rosecloud.api.user.ActivationResendRequest;
import io.rosecloud.api.user.UserActivationInfo;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.common.security.event.LoginSucceededEvent;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.token.JwtPair;
import io.rosecloud.common.security.token.TokenFactory;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.UserActivationService;
import io.rosecloud.system.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/noauth")
public class NoAuthController {

    private final UserActivationService userActivationService;
    private final UserService userService;
    private final TokenFactory tokenFactory;
    private final ApplicationEventPublisher eventPublisher;

    public NoAuthController(UserActivationService userActivationService,
                            UserService userService,
                            TokenFactory tokenFactory,
                            ApplicationEventPublisher eventPublisher) {
        this.userActivationService = userActivationService;
        this.userService = userService;
        this.tokenFactory = tokenFactory;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping("/activate")
    public ApiResponse<UserActivationInfo> check(@RequestParam("activateToken") String activateToken) {
        return ApiResponse.ok(userActivationService.check(activateToken));
    }

    @PostMapping("/activate")
    public ApiResponse<LoginTokenPairResponse> activate(@RequestBody ActivationConfirmRequest request,
                                                        HttpServletRequest http) {
        UserActivationInfo info = userActivationService.confirm(request.activateToken(), request.password());
        SecurityUser securityUser = userService.loadByUsername(info.username())
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_NOT_FOUND));
        JwtPair tokenPair = tokenFactory.createTokenPair(securityUser);

        String ip = resolveIp(http);
        String userAgent = http.getHeader(HttpHeaders.USER_AGENT);

        eventPublisher.publishEvent(new LoginSucceededEvent(securityUser, ip, userAgent));

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

    private record LoginTokenPairResponse(String token, String refreshToken, long expiresIn) {
    }
}
