package io.rosecloud.system.controller;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.system.support.PageSupport;
import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.starter.security.annotation.InternalApi;
import io.rosecloud.system.domain.LoginLog;
import io.rosecloud.system.service.LoginLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * System endpoints for login logs and Feign-facing login log recording.
 */
@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/system/login-logs")
public class LoginLogController {

    private final LoginLogService loginLogService;

    public LoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    @PreAuthorize("hasAuthority('system:loginlog:list')")
    @GetMapping
    public ApiResponse<PageResult<LoginLog>> page(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String username,
                                                  @RequestParam(required = false) Boolean success) {
        return ApiResponse.ok(loginLogService.page(PageSupport.current(current), PageSupport.size(size), username, success));
    }

    @InternalApi
    @PostMapping
    public ApiResponse<Void> record(@RequestBody LoginLogRequest request) {
        loginLogService.record(request);
        return ApiResponse.ok();
    }
}
