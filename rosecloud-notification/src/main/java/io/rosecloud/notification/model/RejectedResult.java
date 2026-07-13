package io.rosecloud.notification.model;

/** Rejected after the concurrency-permit wait timed out; send(...) was not called. */
public record RejectedResult(
        String notificationId, String recipientId, ChannelType channel) implements DeliveryResult {

    public RejectedResult {
        notificationId = DeliveryResult.requireId(notificationId, "notificationId");
        recipientId = DeliveryResult.requireId(recipientId, "recipientId");
        channel = DeliveryResult.requireNonNull(channel, "channel");
    }
}
