package io.rosecloud.starter.trace.gateway;

import io.rosecloud.common.security.SecurityHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Generates a new trace id for every gateway request, injects it into the
 * downstream request, and echoes it on the response.
 */
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = generateTraceId();
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(SecurityHeaders.TRACE_ID, traceId)
                .build();
        exchange.getResponse().getHeaders().set(SecurityHeaders.TRACE_ID, traceId);
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
