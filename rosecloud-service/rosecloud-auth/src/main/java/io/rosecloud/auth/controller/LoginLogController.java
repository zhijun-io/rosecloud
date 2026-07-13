package io.rosecloud.auth.controller;

import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.common.core.model.TimePageQuery;
import io.rosecloud.starter.security.annotation.InternalApi;
import io.rosecloud.auth.domain.LoginLog;
import io.rosecloud.auth.service.LoginLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth-owned login audit log: recorded internally (auth reports on login), listed by admins.
 */
@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/auth/login-logs")
public class LoginLogController {

    private final LoginLogService loginLogService;

    public LoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    @PreAuthorize("hasAuthority('system:loginlog:list')")
    @GetMapping
    public ApiResponse<PagedData<LoginLog>> page(TimePageQuery pageQuery,
                                                 @RequestParam(required = false) String username,
                                                 @RequestParam(required = false) Boolean success) {
        return ApiResponse.ok(loginLogService.page(pageQuery, username, success));
    }

    @InternalApi
    @PostMapping
    public ApiResponse<Void> record(@RequestBody LoginLogRequest request) {
        loginLogService.record(request);
        return ApiResponse.ok();
    }
}
