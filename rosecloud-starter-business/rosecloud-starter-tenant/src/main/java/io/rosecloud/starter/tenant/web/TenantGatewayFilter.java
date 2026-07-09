package io.rosecloud.starter.tenant.web;

import io.rosecloud.common.security.SecurityHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Reactive gateway filter that <em>strips</em> any client-supplied
 * {@link SecurityHeaders#TENANT_ID} header at the trust boundary.
 *
 * <p>Downstream services derive the tenant from the authenticated principal
 * (see {@code TenantWebFilter}), never from this header, so a spoofed
 * {@code X-Tenant-Id} must not reach them.
 */
public class TenantGatewayFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!request.getHeaders().containsHeader(SecurityHeaders.TENANT_ID)) {
            return chain.filter(exchange);
        }
        ServerHttpRequest mutated = request.mutate()
                .headers(headers -> headers.remove(SecurityHeaders.TENANT_ID))
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
