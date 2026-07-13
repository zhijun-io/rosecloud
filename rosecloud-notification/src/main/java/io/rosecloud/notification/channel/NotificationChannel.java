package io.rosecloud.notification.channel;

import io.rosecloud.notification.model.ChannelType;
import io.rosecloud.notification.model.Notification;
import io.rosecloud.notification.model.Recipient;

/**
 * The single required extension point. Each implementation serves one
 * {@link ChannelType}. Channel instances are registered already initialized and
 * reused at engine level, so they must be thread-safe or internally serialized.
 */
public interface NotificationChannel {

    /** Stable, non-null channel identifier; must return consistently across calls. */
    ChannelType type();

    /**
     * Quick, local, side-effect-free capability check. Returning false records a
     * {@code SkippedResult(UNSUPPORTED)}. Must not actually send or do remote probing.
     * If it throws, the engine records a {@code FailedResult(EXCEPTION)} (attempts may be 0).
     * The address can be obtained from {@code r.addresses().get(type())} (always non-blank
     * when invoked, since the engine has already passed the NO_ADDRESS stage).
     */
    default boolean supports(Notification n, Recipient r) {
        return true;
    }

    /**
     * The actual send. The address is obtained from {@code r.addresses().get(type())};
     * the engine has already completed NO_ADDRESS classification, so it is present and
     * non-blank, and the channel need not null-check it.
     *
     * <p>Returns the provider message id; may return {@code null} if the upstream gave no
     * recordable id. A blank return value is illegal and recorded as
     * {@code FailedResult(EXCEPTION)}. Throwing signals failure.
     *
     * <p>The implementation must respond to thread interruption: if it catches
     * {@link InterruptedException} it should restore the interrupt status and exit promptly
     * rather than swallowing the interrupt and continuing to block. A deadline-driven
     * {@code future.cancel(true)} by the engine is ultimately mapped to {@code TimeoutResult}.
     *
     * <p>No checked exception is declared: implementations should wrap checked exceptions in
     * {@link RuntimeException}. {@link NonRetryableException} is the only signal that suppresses
     * retry; all other exceptions are retried per policy.
     */
    String send(Notification n, Recipient r);
}
