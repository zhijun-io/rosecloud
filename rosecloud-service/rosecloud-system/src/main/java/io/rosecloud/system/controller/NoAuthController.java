package io.rosecloud.system.controller;
import lombok.RequiredArgsConstructor;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.starter.security.util.SecurityUtils;
import io.rosecloud.system.service.NoAuthService;
import io.rosecloud.system.service.dto.ActivationConfirmRequest;
import io.rosecloud.system.service.dto.ActivationResendRequest;
import io.rosecloud.system.service.dto.ActivationResult;
import io.rosecloud.system.service.dto.UserActivationInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/noauth")
public class NoAuthController {

    private final NoAuthService noAuthService;

    @GetMapping("/activate")
    public ApiResponse<UserActivationInfo> check(@RequestParam("activateToken") String activateToken) {
        return ApiResponse.ok(noAuthService.check(activateToken));
    }

    @PostMapping("/activate")
    public ApiResponse<ActivationResult> activate(@RequestBody ActivationConfirmRequest request,
                                                  HttpServletRequest http) {
        String ip = SecurityUtils.getClientIp(http);
        String userAgent = SecurityUtils.getUserAgent(http);
        ActivationResult result = noAuthService.activate(request.activateToken(), request.password(), ip, userAgent);
        return ApiResponse.ok(result);
    }

    @PostMapping("/activate/resend")
    public ApiResponse<Void> resend(@RequestBody ActivationResendRequest request) {
        noAuthService.resend(request.username());
        return ApiResponse.ok();
    }
}
