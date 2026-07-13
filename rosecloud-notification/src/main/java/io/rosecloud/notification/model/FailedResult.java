package io.rosecloud.notification.model;

import java.time.Duration;

/** Delivery failed. errorCode / errorMessage are independently optional, not required as a pair. */
public record FailedResult(
        String notificationId, String recipientId, ChannelType channel,
        FailReason reason, int attempts, Duration elapsed,
        String errorCode, String errorMessage) implements DeliveryResult {

    public FailedResult {
        notificationId = DeliveryResult.requireId(notificationId, "notificationId");
        recipientId = DeliveryResult.requireId(recipientId, "recipientId");
        channel = DeliveryResult.requireNonNull(channel, "channel");
        reason = DeliveryResult.requireNonNull(reason, "reason");
        elapsed = DeliveryResult.requireNonNull(elapsed, "elapsed");
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts must not be negative");
        }
        if (reason == FailReason.NON_RETRYABLE && attempts < 1) {
            throw new IllegalArgumentException("NON_RETRYABLE requires attempts >= 1");
        }
        if (reason == FailReason.CHANNEL_UNAVAILABLE && attempts != 0) {
            throw new IllegalArgumentException("CHANNEL_UNAVAILABLE requires attempts == 0");
        }
        errorCode = DeliveryResult.requireNullOrNonBlank(errorCode, "errorCode");
        errorMessage = DeliveryResult.requireNullOrNonBlank(errorMessage, "errorMessage");
    }
}
