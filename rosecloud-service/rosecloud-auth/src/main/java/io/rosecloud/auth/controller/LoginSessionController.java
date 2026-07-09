package io.rosecloud.auth.controller;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.common.security.model.LoginSession;
import io.rosecloud.auth.service.LoginSessionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/auth/sessions")
public class LoginSessionController {

    private final LoginSessionService sessionStoreService;

    public LoginSessionController(LoginSessionService sessionStoreService) {
        this.sessionStoreService = sessionStoreService;
    }

    @PreAuthorize("hasAuthority('system:session:list')")
    @GetMapping("/online")
    public ApiResponse<PageResult<LoginSession>> online(@RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size) {
        List<LoginSession> all = sessionStoreService.findAll();
        long total = all.size();
        int start = (int) ((current - 1) * size);
        int end = Math.min(start + (int) size, all.size());
        List<LoginSession> page = start >= all.size() ? List.of() : all.subList(start, end);
        return ApiResponse.ok(PageResult.of(page, total, current, size));
    }

    @PreAuthorize("hasAuthority('system:session:kick')")
    @DeleteMapping
    public ApiResponse<Void> kick(@RequestParam String sessionId) {
        sessionStoreService.revokeBySessionId(sessionId);
        return ApiResponse.ok();
    }
}
