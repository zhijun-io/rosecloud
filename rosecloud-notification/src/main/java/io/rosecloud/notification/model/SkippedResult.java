package io.rosecloud.notification.model;

/** Delivery skipped; send(...) was not called. */
public record SkippedResult(
        String notificationId, String recipientId, ChannelType channel,
        SkipReason reason) implements DeliveryResult {

    public SkippedResult {
        notificationId = DeliveryResult.requireId(notificationId, "notificationId");
        recipientId = DeliveryResult.requireId(recipientId, "recipientId");
        channel = DeliveryResult.requireNonNull(channel, "channel");
        reason = DeliveryResult.requireNonNull(reason, "reason");
    }
}
