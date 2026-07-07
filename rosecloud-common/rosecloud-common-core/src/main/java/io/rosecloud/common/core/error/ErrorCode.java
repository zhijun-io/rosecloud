package io.rosecloud.common.core.error;

/**
 * Stable error-code contract. Services declare their own enums implementing
 * this so business errors carry a machine-readable code plus a default message.
 *
 * <p>Code is derived as {@code <module>.<enum-name>}, e.g.
 * {@code auth.invalid_token} or {@code system.user_not_found}: {@link #module()}
 * gives the area (derived from the enum class name), the enum constant name
 * gives the specific error, so codes are self-describing and unique without
 * sequence numbers. Codes are part of the API contract — enum names and the
 * {@code <Name>ErrorCode} class naming must not change once released.
 *
 * <p>Implementations must be Java enums; {@link #name()} is satisfied by
 * {@link Enum#name()}.
 */
public interface ErrorCode {

    /** Enum constant name; satisfied by {@link Enum#name()}. */
    String name();

    /**
     * Lowercase module name, derived from the enum class name by stripping the
     * {@code ErrorCode} suffix, e.g. {@code AuthErrorCode} → {@code auth}.
     * Constants with a body are anonymous subclasses, so the enclosing enum
     * class is used.
     */
    default String module() {
        Class<?> type = getClass();
        if (type.isAnonymousClass()) {
            type = type.getEnclosingClass();
        }
        return type.getSimpleName().replace("ErrorCode", "").toLowerCase();
    }

    /**
     * Stable code of the form {@code <module>.<name>}, derived from
     * {@link #module()} and {@link #name()}.
     */
    default String code() {
        return module() + "." + name().toLowerCase();
    }

    String message();

    /**
     * Suggested HTTP status for this error. Defaults to 400 (Bad Request);
     * error codes representing authentication failures should override to 401.
     */
    default int httpStatus() {
        return 400;
    }
}
