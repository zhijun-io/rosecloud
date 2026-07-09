package io.rosecloud.auth.config;

import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.api.user.SystemUserApi;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.event.LoginFailedEvent;
import io.rosecloud.common.security.event.LoginSucceededEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Provides functional-interface beans that bridge the security starter's
 * event-driven callbacks to the system service over Feign clients.
 *
 * <p>Each bean is a standard {@link FunctionalInterface} (Consumer, Function)
 * so no custom interface types are needed in the starter.
 *
 * <p>Login/logout session persistence is handled by {@link io.rosecloud.common.security.session.SessionStore}
 * through the provided implementation; this configuration only bridges the
 * login-log and user-profile side effects.
 */
@Configuration
@ConditionalOnBean(SystemUserApi.class)
public class AuthSecurityConfiguration {

    @Bean
    Consumer<LoginSucceededEvent> loginSucceededHandler(
            LoginLogApi loginLogApi, SystemUserApi systemUserApi) {
        return event -> {
            loginLogApi.record(new LoginLogRequest(event.securityUser().getUsername(), true, null,
                    event.ip(), event.userAgent()));
            systemUserApi.updateLastLoginTime(event.securityUser().getUserId(), LocalDateTime.now(ZoneId.systemDefault()));
        };
    }

    @Bean
    Consumer<LoginFailedEvent> loginFailedHandler(LoginLogApi loginLogApi) {
        return event -> loginLogApi.record(new LoginLogRequest(
                event.username(), false, event.reason(), event.ip(), event.userAgent()));
    }

    @Bean
    Function<String, Optional<SecurityUser>> userLookup(SystemUserApi systemUserApi) {
        return username -> Optional.ofNullable(systemUserApi.loadUserByUsername(username).data());
    }
}
