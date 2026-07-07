package io.rosecloud.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides the password encoder used to hash passwords on user creation. It is
 * {@code @Primary} so that, in the monolith where both this and the auth
 * service's encoder exist, injection resolves to a single instance.
 */
@Configuration
public class SystemSecurityConfiguration {

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
