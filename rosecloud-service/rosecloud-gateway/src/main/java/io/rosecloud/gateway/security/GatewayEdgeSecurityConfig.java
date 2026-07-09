package io.rosecloud.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Edge defence for the {@code X-Internal} trust boundary.
 *
 * <p>Internal services authenticate each other via the {@code X-Internal} header
 * (added by the Feign {@code ServiceAuthRequestInterceptor}); downstream services
 * trust it without further verification. That trust is only safe if the header
 * can never originate from an external client. Since external traffic reaches
 * services through this gateway, we strip the header from every inbound request
 * so a client cannot spoof an internal caller. Internal service-to-service calls
 * travel directly (load-balanced) and never pass through this filter, so they
 * keep the header.
 */
@Configuration
public class GatewayEdgeSecurityConfig {

    /** Must match {@code InternalApiAuthenticationFilter.INTERNAL_HEADER}. */
    private static final String INTERNAL_HEADER = "X-Internal";

    @Bean
    @org.springframework.core.annotation.Order(Ordered.HIGHEST_PRECEDENCE)
    public GlobalFilter stripInboundInternalHeaderFilter() {
        return new GlobalFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                HttpHeaders headers = exchange.getRequest().getHeaders();
                if (headers.getFirst(INTERNAL_HEADER) != null) {
                    ServerWebExchange stripped = exchange.mutate()
                            .request(exchange.getRequest().mutate()
                                    .headers(h -> h.remove(INTERNAL_HEADER))
                                    .build())
                            .build();
                    return chain.filter(stripped);
                }
                return chain.filter(exchange);
            }

            @Override
            public String toString() {
                return "StripInboundInternalHeaderFilter";
            }
        };
    }
}
