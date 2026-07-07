package io.rosecloud.starter.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

/**
 * Verifies the edge white-list is narrowed so that only the health probe and
 * info actuator endpoints remain open, while every other actuator path now
 * requires a JWT. The matching logic mirrors {@code MonolithJwtFilter} and
 * {@code JwtAuthenticationGlobalFilter}.
 */
class PublicPathsPropertiesTest {

    private final PathMatcher matcher = new AntPathMatcher();

    private boolean whiteListed(PublicPathsProperties props, String path) {
        for (String pattern : props.getPublicPaths()) {
            if (matcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void actuatorWildcardRemovedOnlyHealthAndInfoRemain() {
        PublicPathsProperties props = new PublicPathsProperties();
        List<String> paths = props.getPublicPaths();

        // The broad wildcard must be gone.
        assertFalse(paths.contains("/actuator/**"), "broad /actuator/** must be removed");
        // Only the explicitly required actuator endpoints stay public.
        assertTrue(paths.contains("/actuator/health/**"));
        assertTrue(paths.contains("/actuator/info"));
    }

    @Test
    void healthAndInfoBypassJwt() {
        PublicPathsProperties props = new PublicPathsProperties();

        assertTrue(whiteListed(props, "/actuator/health"));
        assertTrue(whiteListed(props, "/actuator/health/liveness"));
        assertTrue(whiteListed(props, "/actuator/health/readiness"));
        assertTrue(whiteListed(props, "/actuator/info"));
    }

    @Test
    void otherActuatorEndpointsRequireJwt() {
        PublicPathsProperties props = new PublicPathsProperties();

        assertFalse(whiteListed(props, "/actuator/env"));
        assertFalse(whiteListed(props, "/actuator/metrics"));
        assertFalse(whiteListed(props, "/actuator/prometheus"));
        assertFalse(whiteListed(props, "/actuator/heapdump"));
        assertFalse(whiteListed(props, "/actuator/"));
    }

    @Test
    void loginAndRefreshRemainPublic() {
        PublicPathsProperties props = new PublicPathsProperties();

        assertTrue(whiteListed(props, "/api/auth/login"));
        assertTrue(whiteListed(props, "/api/auth/refresh"));
    }
}
