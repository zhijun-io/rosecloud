package io.rosecloud.common.security.context;

/**
 * ThreadLocal holder for the {@link CurrentUser} bound to the current request.
 * Populated by the security context filter, cleared on request end.
 */
public final class UserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();

    private UserContext() {
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    /**
     * Raw bearer token of the inbound request, captured so outbound Feign calls
     * can propagate the caller's identity to downstream services.
     */
    public static String getToken() {
        return TOKEN.get();
    }

    public static void setToken(String token) {
        TOKEN.set(token);
    }

    public static void clear() {
        HOLDER.remove();
        TOKEN.remove();
    }
}
