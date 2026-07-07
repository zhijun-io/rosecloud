package io.rosecloud.gateway.security;

import io.rosecloud.starter.security.jwt.JwtTokenCodec;
import io.rosecloud.starter.security.jwt.TokenRevocationService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class GatewaySecurityConfiguration {

    @Bean
    public JwtAuthenticationGlobalFilter jwtAuthenticationGlobalFilter(
            JwtTokenCodec jwtTokenCodec, GatewaySecurityProperties properties,
            TokenRevocationService tokenRevocationService) {
        return new JwtAuthenticationGlobalFilter(jwtTokenCodec, properties, tokenRevocationService);
    }

    @Bean
    public TraceIdGlobalFilter traceIdGlobalFilter() {
        return new TraceIdGlobalFilter();
    }
}
