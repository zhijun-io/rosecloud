package io.rosecloud.starter.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ant-style request paths that bypass JWT verification at the edge (gateway /
 * monolith), e.g. login and refresh. Shared by both deployment shapes so the
 * white-list is not duplicated per runtime.
 *
 * <p>Only the explicitly required actuator endpoints ({@code /actuator/health/**}
 * and {@code /actuator/info}) are whitelisted for health probes / monitoring;
 * every other actuator endpoint requires a valid JWT, closing the gap where the
 * whole {@code /actuator/**} tree was open. The actuator exposure itself is
 * already restricted to {@code health,info} in {@code rosecloud-common.yaml}.
 *
 * <p>Bound to {@code rosecloud.security.public-paths.*}.
 */
@ConfigurationProperties(prefix = "rosecloud.security.public-paths")
public class PublicPathsProperties {

    private List<String> publicPaths = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/actuator/health/**",
            "/actuator/info");

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }
}
