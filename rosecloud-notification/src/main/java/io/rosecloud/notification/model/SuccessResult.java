package io.rosecloud.notification.model;

import java.time.Duration;

/** Channel accepted the notification. attempts >= 1; providerMessageId may be null. */
public record SuccessResult(
        String notificationId, String recipientId, ChannelType channel,
        String providerMessageId, int attempts, Duration elapsed) implements DeliveryResult {

    public SuccessResult {
        notificationId = DeliveryResult.requireId(notificationId, "notificationId");
        recipientId = DeliveryResult.requireId(recipientId, "recipientId");
        channel = DeliveryResult.requireNonNull(channel, "channel");
        elapsed = DeliveryResult.requireNonNull(elapsed, "elapsed");
        if (attempts < 1) {
            throw new IllegalArgumentException("attempts must be >= 1 for SUCCESS");
        }
        providerMessageId = DeliveryResult.requireNullOrNonBlank(providerMessageId, "providerMessageId");
    }
}
