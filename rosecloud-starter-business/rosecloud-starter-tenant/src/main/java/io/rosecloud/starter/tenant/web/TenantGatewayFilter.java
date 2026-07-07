package io.rosecloud.starter.tenant.web;

import io.rosecloud.common.security.SecurityHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Reactive gateway filter that propagates {@link SecurityHeaders#TENANT_ID}
 * to downstream services.
 */
public class TenantGatewayFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String tenantId = request.getHeaders().getFirst(SecurityHeaders.TENANT_ID);
        if (tenantId == null || tenantId.isBlank()) {
            return chain.filter(exchange);
        }
        ServerHttpRequest mutated = request.mutate()
                .header(SecurityHeaders.TENANT_ID, tenantId)
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
