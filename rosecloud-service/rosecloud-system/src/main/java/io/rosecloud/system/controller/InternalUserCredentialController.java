package io.rosecloud.system.controller;

import io.rosecloud.api.user.ActivationConfirmRequest;
import io.rosecloud.api.user.ActivationResendRequest;
import io.rosecloud.api.user.UserActivationInfo;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.system.service.UserActivationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/user-credentials")
public class InternalUserCredentialController {

    private final UserActivationService userActivationService;

    public InternalUserCredentialController(UserActivationService userActivationService) {
        this.userActivationService = userActivationService;
    }

    @GetMapping("/activation/{activateToken}")
    public ApiResponse<UserActivationInfo> check(@PathVariable String activateToken) {
        return ApiResponse.ok(userActivationService.check(activateToken));
    }

    @PostMapping("/activation/confirm")
    public ApiResponse<UserActivationInfo> confirm(@RequestBody ActivationConfirmRequest request) {
        return ApiResponse.ok(userActivationService.confirm(request.activateToken(), request.password()));
    }

    @PostMapping("/activation/resend")
    public ApiResponse<UserActivationInfo> resend(@RequestBody ActivationResendRequest request) {
        return ApiResponse.ok(userActivationService.resend(request.username()));
    }
}
