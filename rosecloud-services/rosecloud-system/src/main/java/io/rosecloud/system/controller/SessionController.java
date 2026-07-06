package io.rosecloud.system.controller;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.system.domain.LoginSession;
import io.rosecloud.system.service.LoginSessionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/system/sessions")
public class SessionController {

    private final LoginSessionService loginSessionService;

    public SessionController(LoginSessionService loginSessionService) {
        this.loginSessionService = loginSessionService;
    }

    @GetMapping("/online")
    public ApiResponse<PageResult<LoginSession>> online(@RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(loginSessionService.onlinePage(current, size));
    }
}
