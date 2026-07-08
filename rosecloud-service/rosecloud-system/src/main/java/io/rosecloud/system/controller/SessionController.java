package io.rosecloud.system.controller;

import io.rosecloud.api.session.TokenRevocationApi;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;
import io.rosecloud.starter.security.session.LoginSession;
import io.rosecloud.starter.security.session.LoginSessionStore;
import io.rosecloud.system.error.SystemErrorCode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/system/sessions")
public class SessionController {

    private final LoginSessionStore sessionStore;
    private final TokenRevocationApi tokenRevocationApi;

    public SessionController(LoginSessionStore sessionStore, TokenRevocationApi tokenRevocationApi) {
        this.sessionStore = sessionStore;
        this.tokenRevocationApi = tokenRevocationApi;
    }

    @PreAuthorize("hasAuthority('system:session:list')")
    @GetMapping("/online")
    public ApiResponse<PageResult<LoginSession>> online(@RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(sessionStore.onlinePage(current, size, scopeTenantId()));
    }

    @PreAuthorize("hasAuthority('system:session:kick')")
    @DeleteMapping
    public ApiResponse<Void> kick(@RequestParam String jti) {
        LoginSession session = sessionStore.findByJti(jti)
                .orElseThrow(() -> new BizException(SystemErrorCode.SESSION_NOT_FOUND));
        sessionStore.markLoggedOut(jti);
        LocalDateTime expireTime = session.expiresAt() == null
                ? null : LocalDateTime.ofInstant(session.expiresAt(), ZoneId.systemDefault());
        tokenRevocationApi.revoke(jti, expireTime);
        return ApiResponse.ok();
    }

    /** Returns the caller's tenant id, or null for platform admins (who see all sessions). */
    private static String scopeTenantId() {
        CurrentUser user = UserContext.get();
        return user == null ? null : user.tenantId();
    }
}
