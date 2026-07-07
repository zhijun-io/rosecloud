package io.rosecloud.starter.security;

import io.rosecloud.common.core.error.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Writes the uniform {@code ApiResponse}-shaped error body for auth failures
 * (401/403). The message is JSON-escaped inline so a quote or backslash in the
 * detail cannot break the JSON envelope and let an attacker smuggle a second
 * field. The security starter deliberately keeps Jackson off this critical
 * path, so escaping is done by hand rather than via an {@code ObjectMapper}.
 *
 * <p>Servlet filters use {@link #write(HttpServletResponse, int, ErrorCode, String)};
 * the reactive gateway filter uses {@link #body(ErrorCode, String)} and writes
 * the resulting string to its {@code DataBuffer}, so escaping stays in one place.
 */
public final class ErrorJson {

    private ErrorJson() {
    }

    /** Serializes the envelope to an escaped JSON string (no status written). */
    public static String body(ErrorCode errorCode, String message) {
        return "{\"success\":false,\"code\":\"" + escape(errorCode.code())
                + "\",\"message\":\"" + escape(message) + "\",\"data\":null}";
    }

    public static void write(HttpServletResponse response, int status, ErrorCode errorCode, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(body(errorCode, message));
    }

    /**
     * Escapes a string per RFC 8259: quote, backslash and the ASCII control
     * range are encoded; everything else (incl. non-ASCII) is passed through
     * untouched since the response charset is UTF-8.
     */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
