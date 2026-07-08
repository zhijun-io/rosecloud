package io.rosecloud.auth.config;

import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.api.user.SystemUserApi;
import io.rosecloud.api.user.UserAuthInfo;
import io.rosecloud.starter.security.login.LoginFailedEvent;
import io.rosecloud.starter.security.login.LoginSucceededEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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
 * <p>Login/logout session persistence is handled by {@link io.rosecloud.starter.security.session.LoginSessionStore}
 * through {@code LoginSessionManager}; this configuration only bridges the
 * login-log and user-profile side effects.
 */
@Configuration
public class AuthSecurityConfiguration {

    @Bean
    public PasswordEncoder authPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    Consumer<LoginSucceededEvent> loginSucceededHandler(
            LoginLogApi loginLogApi, SystemUserApi systemUserApi) {
        return event -> {
            loginLogApi.record(new LoginLogRequest(event.username(), true, null,
                    event.ip(), event.userAgent()));
            systemUserApi.updateLastLoginTime(event.userId(), LocalDateTime.now(ZoneId.systemDefault()));
        };
    }

    @Bean
    Consumer<LoginFailedEvent> loginFailedHandler(LoginLogApi loginLogApi) {
        return event -> loginLogApi.record(new LoginLogRequest(
                event.username(), false, event.reason(), event.ip(), event.userAgent()));
    }

    @Bean
    Function<String, Optional<UserAuthInfo>> userLookup(SystemUserApi systemUserApi) {
        return username -> Optional.ofNullable(systemUserApi.getAuthInfo(username).data());
    }
}
