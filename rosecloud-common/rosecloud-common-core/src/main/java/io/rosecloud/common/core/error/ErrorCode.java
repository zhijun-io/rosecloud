package io.rosecloud.common.core.error;

/**
 * Stable error-code contract. Services declare their own enums implementing
 * this so business errors carry a machine-readable code plus a default message.
 *
 * <p>Code is derived as {@code <module>.<enum-name>}, e.g.
 * {@code auth.invalid_token} or {@code system.user_not_found}: the module prefix
 * ({@link #modulePrefix()}) gives the area, the enum constant name gives the
 * specific error, so codes are self-describing and unique without sequence
 * numbers. Codes are part of the API contract — enum names must not be renamed
 * once released.
 */
public interface ErrorCode {

    /** Lowercase module prefix, e.g. {@code auth}, {@code system}, {@code common}. */
    String modulePrefix();

    /**
     * Stable code of the form {@code <module>.<name>}, derived from
     * {@link #modulePrefix()} and the enum constant name.
     */
    default String code() {
        return modulePrefix() + "." + ((Enum<?>) this).name().toLowerCase();
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
