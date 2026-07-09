package io.rosecloud.auth.config;

import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.api.user.UserApi;
import io.rosecloud.common.security.event.LoginFailedEvent;
import io.rosecloud.common.security.event.LoginSucceededEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.function.Consumer;

/**
 * Provides functional-interface beans that bridge the security starter's
 * event-driven callbacks to the system service over Feign clients.
 *
 * <p>Login/logout session persistence is handled by {@link io.rosecloud.common.security.session.SessionStore}
 * through the provided implementation; this configuration only bridges the
 * login-log and user-profile side effects.
 *
 * <p>Login-log persistence is a best-effort side effect: a failure of the
 * downstream system service must never break the login/refresh flow, so the
 * Feign calls are wrapped and only logged on failure.
 */
@Configuration
public class AuthSecurityConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AuthSecurityConfiguration.class);

    @Bean
    Consumer<LoginSucceededEvent> loginSucceededHandler(LoginLogApi loginLogApi) {
        return event -> recordLoginLog(loginLogApi, new LoginLogRequest(
                event.securityUser().getUsername(), true, null, event.ip(), event.userAgent()));
    }

    @Bean
    Consumer<LoginFailedEvent> loginFailedHandler(LoginLogApi loginLogApi) {
        return event -> recordLoginLog(loginLogApi, new LoginLogRequest(
                event.username(), false, event.reason(), event.ip(), event.userAgent()));
    }

    private static void recordLoginLog(LoginLogApi loginLogApi, LoginLogRequest request) {
        try {
            loginLogApi.record(request);
        } catch (Exception e) {
            log.warn("记录登录日志失败，忽略以不影响登录流程: user={}", request.username(), e);
        }
    }

    @Bean
    UserDetailsService userDetailsService(UserApi userApi) {
        return username -> userApi.loadUserByUsername(username);
    }
}
