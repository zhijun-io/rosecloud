package io.rosecloud.system.controller;

import io.rosecloud.api.user.ActivationConfirmRequest;
import io.rosecloud.api.user.ActivationResendRequest;
import io.rosecloud.api.user.UserActivationInfo;
import io.rosecloud.api.user.UserAuthInfo;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.starter.security.jwt.JwtTokenCodec;
import io.rosecloud.starter.security.login.JwtTokenIssuer;
import io.rosecloud.starter.security.login.JwtTokenPair;
import io.rosecloud.starter.security.login.LoginSucceededEvent;
import io.rosecloud.starter.security.login.LoginTokenPairResponse;
import io.rosecloud.starter.security.web.ClientIpResolver;
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

import java.time.Instant;

@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/noauth")
public class NoAuthController {

    private final UserActivationService userActivationService;
    private final UserService userService;
    private final JwtTokenIssuer jwtTokenIssuer;
    private final JwtTokenCodec jwtTokenCodec;
    private final ApplicationEventPublisher eventPublisher;
    private final ClientIpResolver clientIpResolver;

    public NoAuthController(UserActivationService userActivationService,
                            UserService userService,
                            JwtTokenIssuer jwtTokenIssuer,
                            JwtTokenCodec jwtTokenCodec,
                            ApplicationEventPublisher eventPublisher,
                            ClientIpResolver clientIpResolver) {
        this.userActivationService = userActivationService;
        this.userService = userService;
        this.jwtTokenIssuer = jwtTokenIssuer;
        this.jwtTokenCodec = jwtTokenCodec;
        this.eventPublisher = eventPublisher;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/activate")
    public ApiResponse<UserActivationInfo> check(@RequestParam("activateToken") String activateToken) {
        return ApiResponse.ok(userActivationService.check(activateToken));
    }

    @PostMapping("/activate")
    public ApiResponse<LoginTokenPairResponse> activate(@RequestBody ActivationConfirmRequest request,
                                                        HttpServletRequest http) {
        UserActivationInfo info = userActivationService.confirm(request.activateToken(), request.password());

        UserAuthInfo userAuthInfo = userService.findAuthInfo(info.username())
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_NOT_FOUND));

        CurrentUser currentUser = new CurrentUser(
                userAuthInfo.userId(),
                userAuthInfo.username(),
                userAuthInfo.tenantId(),
                userAuthInfo.roles(),
                userAuthInfo.perms()
        );

        JwtTokenPair tokenPair = jwtTokenIssuer.issue(currentUser);
        LoginTokenPairResponse tokenResponse = new LoginTokenPairResponse(
                tokenPair.accessToken(), tokenPair.refreshToken(), tokenPair.expiresIn());

        var claims = jwtTokenCodec.parse(tokenPair.accessToken());
        String jti = claims.jti();
        Instant expiresAt = claims.expiresAt();

        String ip = clientIpResolver.resolve(http);
        String userAgent = http.getHeader(HttpHeaders.USER_AGENT);
        eventPublisher.publishEvent(new LoginSucceededEvent(
                userAuthInfo.userId(),
                userAuthInfo.username(),
                userAuthInfo.tenantId(),
                jti,
                expiresAt,
                ip,
                userAgent
        ));

        return ApiResponse.ok(tokenResponse);
    }

    @PostMapping("/activate/resend")
    public ApiResponse<UserActivationInfo> resend(@RequestBody ActivationResendRequest request) {
        return ApiResponse.ok(userActivationService.resend(request.username()));
    }
}
