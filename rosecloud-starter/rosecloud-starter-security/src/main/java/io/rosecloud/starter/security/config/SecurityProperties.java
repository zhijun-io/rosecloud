package io.rosecloud.starter.security.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

import static io.rosecloud.common.core.model.ServiceMetadata.API_PREFIX;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "rosecloud.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();

    @Min(60)
    private long accessTokenExpirationSeconds = 3600;

    @Min(60)
    private long refreshTokenExpirationSeconds = 86400;

    // M4: `/error` is intentionally NOT in the default public list. Spring Boot forwards
    // unauthenticated errors to `/error`; leaving it permitAll can leak error bodies before
    // authentication. With it authenticated, such requests get a normal 401 instead.
    // `/actuator/health/**` stays permitAll for infra probes but MUST sit behind a network
    // policy (do not expose the actuator publicly). Override via `rosecloud.security.public-paths`.
    private String[] publicPaths = {API_PREFIX + "/auth/login", API_PREFIX + "/auth/refresh", API_PREFIX + "/auth/logout",
            API_PREFIX + "/auth/sessions/internal/**", API_PREFIX + "/noauth/**", API_PREFIX + "/public/**",
            "/actuator/health/**"};

    private Cors cors = new Cors();

    // L3: single static shared secret for internal (machine-to-machine) calls. Constant-time
    // comparison is used on the verify side. There is no rotation and no per-service credential;
    // acceptable given the gateway strips inbound X-Internal, but guard the leaked-secret blast
    // radius and never log this value.
    private String internalToken;

    private BruteForce bruteForce = new BruteForce();
    private TokenBinding tokenBinding = new TokenBinding();
    private TenantWriteGuard tenantWriteGuard = new TenantWriteGuard();

    /**
     * CORS settings. Origins must be listed explicitly — a wildcard {@code *} is never
     * combined with credentials, which the browser rejects and which is unsafe. When no
     * origins are configured, cross-origin browser requests are simply not permitted.
     */
    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
        private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        private List<String> allowedHeaders = new ArrayList<>(List.of("Authorization", "Content-Type", "X-Requested-With"));
        private boolean allowCredentials = true;
        private long maxAge = 3600;
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private String issuer = "rosecloud";

        // M2: audience claim. Tokens are minted with this `aud` and the parser requires it,
        // so a token issued for one audience cannot be replayed against a service expecting
        // a different one. Defaults to the shared issuer value.
        private String audience = "rosecloud";
    }

    /**
     * H3: brute-force / credential-stuffing protection for the login and refresh endpoints.
     * Counts consecutive failures per account; once {@link #maxFailedAttempts} is reached the
     * account is locked for {@link #lockoutDurationSeconds}. Backed by Redis when available;
     * without Redis the protection is a no-op (and a warning is logged).
     */
    @Getter
    @Setter
    public static class BruteForce {
        private boolean enabled = true;

        @Min(1)
        private int maxFailedAttempts = 5;

        @Min(10)
        private long lockoutDurationSeconds = 900;
    }

    /**
     * M3: optional device binding. When enabled, a hash of the client IP + User-Agent is
     * embedded in the token at mint time and verified on every request, so a stolen bearer
     * token cannot be replayed from a different device. Tokens minted with binding carry an
     * `fp` claim and are always verified regardless of this flag (it only controls minting).
     */
    @Getter
    @Setter
    public static class TokenBinding {
        private boolean enabled = false;
    }

    /**
     * M5: tenant write-guard strictness. When {@link #failClosed} is true and the tenant
     * status cannot be resolved (no user API, or the lookup failed), mutating requests are
     * blocked instead of allowed. Default false preserves the original fail-open behaviour
     * for services that legitimately have no user API.
     */
    @Getter
    @Setter
    public static class TenantWriteGuard {
        private boolean failClosed = false;
    }
}
