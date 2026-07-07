package io.rosecloud.system.controller;

import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.system.service.LoginLogService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Internal endpoint for the auth service to report login attempts. Not gateway-routed. */
@RestController
@RequestMapping("/internal/login-logs")
public class InternalLoginLogController {

    private final LoginLogService loginLogService;

    public InternalLoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    @PostMapping
    public ApiResponse<Void> record(@RequestBody LoginLogRequest request) {
        loginLogService.record(request);
        return ApiResponse.ok();
    }
}
