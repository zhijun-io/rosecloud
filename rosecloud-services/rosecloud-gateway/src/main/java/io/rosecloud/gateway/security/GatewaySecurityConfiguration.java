package io.rosecloud.gateway.security;

import io.rosecloud.starter.security.jwt.JwtTokenCodec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class GatewaySecurityConfiguration {

    @Bean
    public JwtAuthenticationGlobalFilter jwtAuthenticationGlobalFilter(
            JwtTokenCodec jwtTokenCodec, GatewaySecurityProperties properties) {
        return new JwtAuthenticationGlobalFilter(jwtTokenCodec, properties);
    }
}
