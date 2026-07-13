package io.rosecloud.auth.config;

import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.api.user.UserApi;
import io.rosecloud.auth.service.CredentialService;
import io.rosecloud.auth.service.LoginLogService;
import io.rosecloud.common.security.credential.AuthCredential;
import io.rosecloud.common.security.event.LoginFailedEvent;
import io.rosecloud.common.security.event.LoginSucceededEvent;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import io.rosecloud.common.security.model.SecurityUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.function.Consumer;

/**
 * Provides functional-interface beans that bridge the security starter's event-driven
 * callbacks to auth-local services.
 *
 * <p>Login/logout session persistence is handled by {@link io.rosecloud.common.security.session.SessionStore}
 * through the provided implementation; login-log recording and the combined {@code UserDetailsService}
 * use auth-owned services directly (no cross-service call for the auth service's own data).
 */
@Configuration
public class AuthSecurityConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AuthSecurityConfiguration.class);

    @Bean
    Consumer<LoginSucceededEvent> loginSucceededHandler(LoginLogService loginLogService) {
        return event -> recordLoginLog(loginLogService, new LoginLogRequest(
                event.securityUser().getUsername(), true, null, event.ip(), event.userAgent()));
    }

    @Bean
    Consumer<LoginFailedEvent> loginFailedHandler(LoginLogService loginLogService) {
        return event -> recordLoginLog(loginLogService, new LoginLogRequest(
                event.username(), false, event.reason(), event.ip(), event.userAgent()));
    }

    private static void recordLoginLog(LoginLogService loginLogService, LoginLogRequest request) {
        try {
            loginLogService.record(request);
        } catch (Exception e) {
            log.warn("记录登录日志失败，忽略以不影响登录流程: user={}", request.username(), e);
        }
    }

    /**
     * Combines the auth-owned credential (password hash + auth status) with the system-owned
     * profile and authorities. The password is read only from auth's {@code CredentialService};
     * the system service no longer holds or returns password hashes. A missing credential or
     * profile surfaces as {@link BadCredentialsException} so the login flow stays uniform.
     */
    @Bean
    UserDetailsService userDetailsService(UserApi userApi, CredentialService credentialService) {
        return username -> {
            SecurityUser profile = userApi.loadUserByUsername(username);
            if (profile == null) {
                throw new BadCredentialsException(SecurityErrorCode.BAD_CREDENTIALS.message());
            }
            AuthCredential credential = credentialService.findByUserId(profile.getUserId())
                    .orElseThrow(() -> new BadCredentialsException(SecurityErrorCode.BAD_CREDENTIALS.message()));
            return new SecurityUser(profile.getUserId(), profile.getUsername(), profile.getNickname(),
                    credential.passwordHash(), profile.isEnabled() && credential.enabled(),
                    profile.getTenantId(), profile.getUserPrincipal(), profile.getAuthorities());
        };
    }
}
