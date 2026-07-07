package io.rosecloud.system.controller;

import io.rosecloud.api.session.LoginSessionRequest;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.system.service.LoginSessionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Internal endpoints for the auth service to track login sessions. Not gateway-routed. */
@RestController
@RequestMapping("/internal/sessions")
public class InternalSessionController {

    private final LoginSessionService loginSessionService;

    public InternalSessionController(LoginSessionService loginSessionService) {
        this.loginSessionService = loginSessionService;
    }

    @PostMapping
    public ApiResponse<Void> record(@RequestBody LoginSessionRequest request) {
        loginSessionService.record(request);
        return ApiResponse.ok();
    }

    @PostMapping("/logout-by-jti")
    public ApiResponse<Void> logoutByJti(@RequestParam("jti") String jti) {
        loginSessionService.logoutByJti(jti);
        return ApiResponse.ok();
    }
}
