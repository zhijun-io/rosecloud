package io.rosecloud.auth.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Resolves the originating client IP for audit logging.
 *
 * <p>The {@code X-Forwarded-For} header is only honoured when the immediate TCP
 * peer is a trusted proxy (loopback or site-local, i.e. our own gateway /
 * internal network). Requests that reach the service directly from an
 * untrusted address ignore the header entirely and use the real TCP peer, so
 * a client cannot spoof the recorded IP by forging {@code X-Forwarded-For}.
 */
@Component
public class ClientIpResolver {

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return remoteAddr;
    }

    private static boolean isTrustedProxy(String addr) {
        if (addr == null || addr.isBlank()) {
            return false;
        }
        // Strip an IPv6 scope / zone identifier (e.g. fe80::1%eth0).
        int pct = addr.indexOf('%');
        if (pct >= 0) {
            addr = addr.substring(0, pct);
        }
        try {
            InetAddress inet = InetAddress.getByName(addr);
            return inet.isLoopbackAddress() || inet.isSiteLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
