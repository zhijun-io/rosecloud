package io.rosecloud.notification.model;

import java.time.Duration;

/** Deadline exhausted. attempts may be 0 (timed out before first send) or >= 1. */
public record TimeoutResult(
        String notificationId, String recipientId, ChannelType channel,
        int attempts, Duration elapsed) implements DeliveryResult {

    public TimeoutResult {
        notificationId = DeliveryResult.requireId(notificationId, "notificationId");
        recipientId = DeliveryResult.requireId(recipientId, "recipientId");
        channel = DeliveryResult.requireNonNull(channel, "channel");
        elapsed = DeliveryResult.requireNonNull(elapsed, "elapsed");
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts must not be negative");
        }
    }
}
