package io.rosecloud.starter.tenant.web;

import io.rosecloud.starter.tenant.core.TenantProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Reactive gateway filter that propagates the tenant id header to downstream
 * services. Extend to resolve from host/path when a header is absent.
 */
public class TenantGatewayFilter implements GlobalFilter, Ordered {

    private final TenantProperties properties;

    public TenantGatewayFilter(TenantProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String tenantId = request.getHeaders().getFirst(properties.getHeaderName());
        if (tenantId == null || tenantId.isBlank()) {
            return chain.filter(exchange);
        }
        ServerHttpRequest mutated = request.mutate()
                .header(properties.getHeaderName(), tenantId)
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
