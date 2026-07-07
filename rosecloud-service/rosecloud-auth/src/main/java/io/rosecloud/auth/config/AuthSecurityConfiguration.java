package io.rosecloud.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides the password encoder used by the auth service to verify credentials.
 * Bean is named distinctly so it does not clash with the system service's
 * encoder when both run in the monolith (the system one is {@code @Primary}).
 */
@Configuration
public class AuthSecurityConfiguration {

    @Bean
    public PasswordEncoder authPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
