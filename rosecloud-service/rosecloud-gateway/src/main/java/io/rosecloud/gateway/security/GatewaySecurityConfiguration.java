package io.rosecloud.gateway.security;

import io.rosecloud.starter.security.PublicPathsProperties;
import io.rosecloud.starter.security.jwt.JwtTokenCodec;
import io.rosecloud.starter.security.jwt.TokenRevocationService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PublicPathsProperties.class)
public class GatewaySecurityConfiguration {

    @Bean
    public JwtAuthenticationGlobalFilter jwtAuthenticationGlobalFilter(
            JwtTokenCodec jwtTokenCodec, PublicPathsProperties properties,
            TokenRevocationService tokenRevocationService) {
        return new JwtAuthenticationGlobalFilter(jwtTokenCodec, properties, tokenRevocationService);
    }
}
