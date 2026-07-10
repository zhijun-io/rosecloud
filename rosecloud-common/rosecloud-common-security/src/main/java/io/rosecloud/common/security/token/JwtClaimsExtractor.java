package io.rosecloud.common.security.token;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Reads the {@code jti} (JWT id) and {@code exp} (expiry) claims from a token's
 * payload <em>without verifying its signature</em>.
 *
 * <p>This is intentionally dependency-free: a revocation store must be able to
 * record a token's {@code jti} on logout / rotation / user-disable without
 * holding the signing key, so we decode only the base64url payload segment and
 * pick out the two well-known claims. The token is assumed to be a compact JWT
 * ({@code header.payload.signature}); malformed input yields {@link Optional#empty()}
 * so callers can safely treat an unreadable token as "not revocable".
 */
public final class JwtClaimsExtractor {

    private JwtClaimsExtractor() {
    }

    /**
     * The minimal information a revocation store needs to invalidate a token:
     * its {@code jti} and the instant it naturally expires.
     *
     * @param jti       the JWT id claim (never blank when present)
     * @param expiresAt the {@code exp} claim as an {@link Instant}, or {@code null}
     *                  when the token carries no expiry
     */
    public record RevocationTarget(String jti, Instant expiresAt) {
    }

    public static Optional<RevocationTarget> extract(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return Optional.empty();
        }
        String json;
        try {
            json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String jti = readString(json, "jti");
        if (jti == null || jti.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new RevocationTarget(jti, readExpiry(json)));
    }

    /**
     * Reads a single string claim (e.g. a device fingerprint) from the token payload without
     * verifying the signature. Returns {@link Optional#empty()} for malformed input or a missing
     * claim so callers can treat an unreadable/missing claim as "absent".
     */
    public static Optional<String> readStringClaim(String token, String claim) {
        if (token == null || token.isBlank() || claim == null) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return Optional.empty();
        }
        try {
            String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return Optional.ofNullable(readString(json, claim));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String readString(String json, String key) {
        String keyToken = "\"" + key + "\"";
        int idx = json.indexOf(keyToken);
        if (idx < 0) {
            return null;
        }
        int colon = json.indexOf(':', idx + keyToken.length());
        if (colon < 0) {
            return null;
        }
        int open = json.indexOf('"', colon);
        if (open < 0) {
            return null;
        }
        int close = json.indexOf('"', open + 1);
        if (close < 0) {
            return null;
        }
        return json.substring(open + 1, close);
    }

    private static Instant readExpiry(String json) {
        String keyToken = "\"exp\"";
        int idx = json.indexOf(keyToken);
        if (idx < 0) {
            return null;
        }
        int colon = json.indexOf(':', idx + keyToken.length());
        if (colon < 0) {
            return null;
        }
        int i = colon + 1;
        int len = json.length();
        while (i < len && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        int start = i;
        while (i < len && Character.isDigit(json.charAt(i))) {
            i++;
        }
        if (i == start) {
            return null;
        }
        try {
            long exp = Long.parseLong(json.substring(start, i));
            return Instant.ofEpochSecond(exp);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
