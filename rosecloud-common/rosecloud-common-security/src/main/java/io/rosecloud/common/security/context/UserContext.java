package io.rosecloud.common.security.context;

/**
 * ThreadLocal holder for the {@link CurrentUser} bound to the current request.
 * Populated by the security context filter, cleared on request end.
 */
public final class UserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static void clear() {
        HOLDER.remove();
    }
}
