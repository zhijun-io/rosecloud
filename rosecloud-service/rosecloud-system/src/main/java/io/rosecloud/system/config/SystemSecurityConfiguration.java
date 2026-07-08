package io.rosecloud.system.config;

import io.rosecloud.starter.security.session.JdbcLoginSessionStore;
import io.rosecloud.starter.security.session.LoginSessionStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
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

    /**
     * System-provided database backend for {@link LoginSessionStore}: the JDBC
     * store (from the security starter) pointed at {@code sys_login_session}.
     * Creates the table on startup when missing, so it also backs microservices
     * that share this database. Overridable; the in-memory default and the
     * {@code type=jdbc} auto-configuration both step aside via
     * {@link ConditionalOnMissingBean}.
     */
    @Bean
    @ConditionalOnMissingBean
    public LoginSessionStore loginSessionStore(JdbcTemplate jdbcTemplate) {
        return new JdbcLoginSessionStore(jdbcTemplate, "sys_login_session");
    }
}
