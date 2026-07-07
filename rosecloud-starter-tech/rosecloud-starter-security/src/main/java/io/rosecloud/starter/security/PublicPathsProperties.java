package io.rosecloud.starter.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ant-style request paths that bypass JWT verification at the edge (gateway /
 * monolith), e.g. login, refresh and actuator. Shared by both deployment shapes
 * so the white-list is not duplicated per runtime.
 *
 * <p>Bound to {@code rosecloud.security.public-paths.*}.
 */
@ConfigurationProperties(prefix = "rosecloud.security.public-paths")
public class PublicPathsProperties {

    private List<String> publicPaths = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/actuator/**");

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }
}
