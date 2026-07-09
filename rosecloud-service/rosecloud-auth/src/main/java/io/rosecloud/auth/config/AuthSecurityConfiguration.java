package io.rosecloud.auth.config;

import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.api.user.AuthUserInfo;
import io.rosecloud.api.user.SystemUserApi;
import io.rosecloud.common.security.event.LoginFailedEvent;
import io.rosecloud.common.security.event.LoginSucceededEvent;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.session.SessionStore;
import io.rosecloud.starter.security.session.RedisSessionStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.function.Consumer;

/**
 * Provides functional-interface beans that bridge the security starter's
 * event-driven callbacks to the system service over Feign clients.
 *
 * <p>Login/logout session persistence is handled by {@link io.rosecloud.common.security.session.SessionStore}
 * through the provided implementation; this configuration only bridges the
 * login-log and user-profile side effects.
 */
@Configuration
public class AuthSecurityConfiguration {

    @Bean
    Consumer<LoginSucceededEvent> loginSucceededHandler(LoginLogApi loginLogApi) {
        return event -> {
            loginLogApi.record(new LoginLogRequest(event.securityUser().getUsername(), true, null,
                    event.ip(), event.userAgent()));
        };
    }

    @Bean
    Consumer<LoginFailedEvent> loginFailedHandler(LoginLogApi loginLogApi) {
        return event -> loginLogApi.record(new LoginLogRequest(
                event.username(), false, event.reason(), event.ip(), event.userAgent()));
    }

    @Bean
    SessionStore sessionStore(StringRedisTemplate redisTemplate) {
        return new RedisSessionStore(redisTemplate);
    }

    @Bean
    UserDetailsService userDetailsService(SystemUserApi systemUserApi) {
        return username -> {
            return systemUserApi.loadUserByUsername(username).data();
        };
    }

    private static SecurityUser toSecurityUser(AuthUserInfo authUserInfo) {
        return new SecurityUser(
                authUserInfo.userId(),
                authUserInfo.username(),
                authUserInfo.nickname(),
                authUserInfo.encodedPassword(),
                authUserInfo.enabled(),
                authUserInfo.userPrincipal(),
                authUserInfo.authorities().stream()
                        .map(SimpleGrantedAuthority::new)
                        .map(authority -> (GrantedAuthority) authority)
                        .toList()
        );
    }
}
