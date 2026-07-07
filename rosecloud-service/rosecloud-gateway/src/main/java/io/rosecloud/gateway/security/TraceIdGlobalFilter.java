package io.rosecloud.gateway.security;

import io.rosecloud.common.security.SecurityHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Ensures every request carries a trace id: reuses an inbound
 * {@code X-Trace-Id} or generates one, then propagates it to downstream
 * services and echoes it on the response. Runs before authentication so
 * white-listed requests are traced too.
 */
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(SecurityHeaders.TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
        }
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(SecurityHeaders.TRACE_ID, traceId)
                .build();
        exchange.getResponse().getHeaders().add(SecurityHeaders.TRACE_ID, traceId);
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
