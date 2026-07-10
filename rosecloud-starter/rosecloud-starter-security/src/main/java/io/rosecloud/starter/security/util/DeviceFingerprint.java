package io.rosecloud.starter.security.util;

import io.rosecloud.common.security.token.JwtClaimsExtractor;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Device fingerprinting for optional token binding (M3).
 *
 * <p>A fingerprint is a SHA-256 hash of the client IP and User-Agent. When token binding is
 * enabled the minted token carries the fingerprint in an {@code fp} claim; a stolen bearer
 * token replayed from a different device will not match and is rejected. Verification is
 * intrinsic to the token: a token that carries an {@code fp} claim is always checked,
 * regardless of the current global toggle (which only controls whether new tokens are bound).
 */
public final class DeviceFingerprint {

    private DeviceFingerprint() {
    }

    private static final String FP_CLAIM = "fp";

    /** Computes the fingerprint for the current request. */
    public static String compute(HttpServletRequest request) {
        String ip = request == null ? "" : request.getRemoteAddr();
        String ua = "";
        if (request != null) {
            String header = request.getHeader("User-Agent");
            ua = header == null ? "" : header;
        }
        return hash(ip + "|" + ua);
    }

    /**
     * Verifies the device binding of a (signature-verified) token against the current request.
     * Returns {@code true} when the token carries no {@code fp} claim (not bound) or when the
     * claim matches the request fingerprint. Returns {@code false} on a mismatch.
     */
    public static boolean verify(HttpServletRequest request, String rawToken) {
        return JwtClaimsExtractor.readStringClaim(rawToken, FP_CLAIM)
                .map(fp -> MessageDigest.isEqual(
                        fp.getBytes(StandardCharsets.UTF_8),
                        compute(request).getBytes(StandardCharsets.UTF_8)))
                .orElse(true);
    }

    private static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
