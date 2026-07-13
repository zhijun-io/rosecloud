package io.rosecloud.notification.model;

/**
 * Standardized delivery result. Sealed interface + record subtypes: each result
 * type carries only the fields it needs, so illegal states (SUCCESS with
 * errorCode, SKIPPED with attempts) cannot be expressed at the type level and
 * need no runtime validation matrix.
 */
public sealed interface DeliveryResult
        permits SuccessResult, SkippedResult, FailedResult, TimeoutResult, RejectedResult {

    String notificationId();
    String recipientId();
    ChannelType channel();

    default boolean isSuccess() { return this instanceof SuccessResult; }
    default boolean isSkipped() { return this instanceof SkippedResult; }
    /** 未成功且未跳过：覆盖 FailedResult / TimeoutResult / RejectedResult。 */
    default boolean isFailure() { return !isSuccess() && !isSkipped(); }

    static String requireId(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(name + " must not have surrounding whitespace");
        }
        return value;
    }

    static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }

    static String requireNullOrNonBlank(String value, String name) {
        if (value != null && value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
