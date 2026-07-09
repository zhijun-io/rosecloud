package io.rosecloud.auth.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Feign client scanning for shared API contracts.
 */
@Configuration
@EnableFeignClients(basePackages = "io.rosecloud.api")
public class AuthCoreConfiguration {
}
