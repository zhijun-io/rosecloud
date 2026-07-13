package io.rosecloud.auth.controller;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.common.security.model.LoginSession;
import io.rosecloud.auth.service.LoginSessionService;
import io.rosecloud.starter.data.PagedResults;
import io.rosecloud.starter.security.annotation.InternalApi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/auth/sessions")
public class LoginSessionController {

    private final LoginSessionService sessionStoreService;

    @PreAuthorize("hasAuthority('system:session:list')")
    @GetMapping("/online")
    public ApiResponse<PagedData<LoginSession>> online(PageQuery pageQuery) {
        List<LoginSession> all = sessionStoreService.findAll();
        return ApiResponse.ok(PagedResults.slice(all, pageQuery));
    }

    @PreAuthorize("hasAuthority('system:session:kick')")
    @DeleteMapping
    public ApiResponse<Void> kick(@RequestParam String sessionId) {
        sessionStoreService.revokeBySessionId(sessionId);
        return ApiResponse.ok();
    }

    @InternalApi
    @PostMapping("/internal")
    public void save(@RequestBody LoginSession session) {
        sessionStoreService.save(session);
    }

    @InternalApi
    @GetMapping("/internal/revoked")
    public boolean isRevoked(@RequestParam String token) {
        return sessionStoreService.isRevoked(token);
    }

    @InternalApi
    @PostMapping("/internal/revoke")
    public void revoke(@RequestBody String token) {
        sessionStoreService.revoke(token);
    }

    @InternalApi
    @PostMapping("/internal/revoke/session/{sessionId}")
    public void revokeBySessionId(@PathVariable String sessionId) {
        sessionStoreService.revokeBySessionId(sessionId);
    }

    @InternalApi
    @PostMapping("/internal/revoke/user/{userId}")
    public void revokeByUserId(@PathVariable Long userId) {
        sessionStoreService.revokeByUserId(userId);
    }
}
