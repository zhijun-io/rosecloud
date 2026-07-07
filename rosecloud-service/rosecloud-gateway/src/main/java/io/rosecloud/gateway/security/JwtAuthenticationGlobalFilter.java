package io.rosecloud.gateway.security;

import io.rosecloud.common.security.SecurityHeaders;
import io.rosecloud.starter.security.jwt.TokenRevocationService;
import io.rosecloud.starter.security.jwt.InvalidTokenException;
import io.rosecloud.starter.security.jwt.JwtTokenCodec;
import io.rosecloud.starter.security.jwt.TokenClaims;
import io.rosecloud.starter.security.jwt.TokenType;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;

/**
 * Verifies the bearer JWT on every non-white-listed request and injects the
 * decoded identity for access control. Downstream servlet services decode the
 * same bearer token themselves, so no user headers are injected.
 */
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private final JwtTokenCodec jwtTokenCodec;
    private final GatewaySecurityProperties properties;
    private final TokenRevocationService tokenRevocationService;
    private final PathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationGlobalFilter(JwtTokenCodec jwtTokenCodec, GatewaySecurityProperties properties,
                                         TokenRevocationService tokenRevocationService) {
        this.jwtTokenCodec = jwtTokenCodec;
        this.properties = properties;
        this.tokenRevocationService = tokenRevocationService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (isWhiteListed(request.getPath().value())) {
            return chain.filter(exchange);
        }
        String token = extractToken(request);
        if (token == null) {
            return unauthorized(exchange, "missing token");
        }
        TokenClaims claims;
        try {
            claims = jwtTokenCodec.parse(token);
        } catch (InvalidTokenException e) {
            return unauthorized(exchange, "invalid token");
        }
        if (claims.type() != TokenType.ACCESS) {
            return unauthorized(exchange, "wrong token type");
        }
        if (claims.jti() != null) {
            return Mono.fromCallable(() -> tokenRevocationService.isRevoked(claims.jti()))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(revoked -> revoked
                            ? unauthorized(exchange, "token revoked")
                            : chain.filter(exchange));
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isWhiteListed(String path) {
        for (String pattern : properties.getWhiteList()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private static String extractToken(ServerHttpRequest request) {
        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        return null;
    }

    private static Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"success\":false,\"code\":\"AUTHA003\",\"message\":\""
                + message + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
