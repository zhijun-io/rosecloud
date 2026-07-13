package io.rosecloud.notification.core;

import io.rosecloud.notification.channel.NonRetryableException;
import io.rosecloud.notification.channel.NotificationChannel;
import io.rosecloud.notification.model.BroadcastResult;
import io.rosecloud.notification.model.ChannelType;
import io.rosecloud.notification.model.DeliveryResult;
import io.rosecloud.notification.model.FailedResult;
import io.rosecloud.notification.model.FailReason;
import io.rosecloud.notification.model.Notification;
import io.rosecloud.notification.model.Recipient;
import io.rosecloud.notification.model.SkipReason;
import io.rosecloud.notification.model.SkippedResult;
import io.rosecloud.notification.model.SuccessResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NotificationEngineTest {

    private static final ChannelType EMAIL = ChannelType.EMAIL;
    private static final ChannelType SMS = new ChannelType("SMS");
    private static final ChannelType PUSH = new ChannelType("PUSH");

    private static DeliveryPolicy fanOut() {
        return DeliveryPolicy.fanOut()
                .deadline(Duration.ofSeconds(2))
                .acquireTimeout(Duration.ofMillis(500))
                .maxAttempts(3)
                .baseBackoff(Duration.ofMillis(5))
                .maxConcurrency(8)
                .build();
    }

    private static DeliveryPolicy failover(List<ChannelType> order) {
        return DeliveryPolicy.failover(order)
                .deadline(Duration.ofSeconds(2))
                .acquireTimeout(Duration.ofMillis(500))
                .maxAttempts(3)
                .baseBackoff(Duration.ofMillis(5))
                .maxConcurrency(8)
                .build();
    }

    private static Notification notification(Set<ChannelType> channels) {
        return new Notification("ntf-1", Map.of("text", "hi"), channels);
    }

    private static Recipient recipient(String id, ChannelType channel, String address) {
        return new Recipient(id, Map.of(channel, address), Map.of());
    }

    private static Recipient recipient(String id, Map<ChannelType, String> addresses) {
        return new Recipient(id, addresses, Map.of());
    }

    static final class FakeChannel implements NotificationChannel {
        private final ChannelType type;
        private final java.util.concurrent.atomic.AtomicInteger sends = new java.util.concurrent.atomic.AtomicInteger();
        private Behavior behavior = Behavior.SUCCEED;

        enum Behavior { SUCCEED, FAIL, NON_RETRYABLE, BLANK }

        FakeChannel(ChannelType type) { this.type = type; }
        FakeChannel behave(Behavior b) { this.behavior = b; return this; }
        int sendCount() { return sends.get(); }

        @Override
        public ChannelType type() { return type; }

        @Override
        public String send(Notification n, Recipient r) {
            sends.incrementAndGet();
            return switch (behavior) {
                case SUCCEED -> r.recipientId() + "@ok";
                case FAIL -> throw new RuntimeException("transient failure");
                case NON_RETRYABLE -> throw new NonRetryableException("permanent");
                case BLANK -> "   ";
            };
        }
    }

    static final class FlakeyChannel implements NotificationChannel {
        private final ChannelType type;
        private final java.util.concurrent.atomic.AtomicInteger sends = new java.util.concurrent.atomic.AtomicInteger();
        private final int failFirst;

        FlakeyChannel(ChannelType type, int failFirst) { this.type = type; this.failFirst = failFirst; }
        int sendCount() { return sends.get(); }

        @Override
        public ChannelType type() { return type; }

        @Override
        public String send(Notification n, Recipient r) {
            if (sends.incrementAndGet() <= failFirst) {
                throw new RuntimeException("flakey failure " + sends.get());
            }
            return "ok";
        }
    }

    @Test
    void emptyRecipientsReturnsEmptyResult() {
        FakeChannel email = new FakeChannel(EMAIL);
        try (var engine = NotificationEngine.builder().policy(fanOut()).registerChannel(email).build()) {
            BroadcastResult result = engine.broadcast(notification(Set.of(EMAIL)), List.of());
            assertTrue(result.results().isEmpty());
            assertEquals(0, result.successCount());
            assertEquals("ntf-1", result.notificationId());
        }
    }

    @Test
    void duplicateRecipientIdRejected() {
        FakeChannel email = new FakeChannel(EMAIL);
        try (var engine = NotificationEngine.builder().policy(fanOut()).registerChannel(email).build()) {
            Recipient r1 = recipient("u1", EMAIL, "a@x.com");
            Recipient r2 = recipient("u1", EMAIL, "b@x.com");
            assertThrows(IllegalArgumentException.class,
                    () -> engine.broadcast(notification(Set.of(EMAIL)), List.of(r1, r2)));
        }
    }

    @Test
    void broadcastAfterCloseThrows() {
        FakeChannel email = new FakeChannel(EMAIL);
        var engine = NotificationEngine.builder().policy(fanOut()).registerChannel(email).build();
        engine.close();
        engine.close(); // idempotent
        assertThrows(IllegalStateException.class,
                () -> engine.broadcast(notification(Set.of(EMAIL)), List.of(recipient("u1", EMAIL, "a@x.com"))));
    }

    @Test
    void fanOutDeliversAllChannelsAndOrdersByName() {
        FakeChannel sms = new FakeChannel(SMS);
        FakeChannel email = new FakeChannel(EMAIL);
        try (var engine = NotificationEngine.builder().policy(fanOut())
                .registerChannel(sms).registerChannel(email).build()) {
            Recipient r = recipient("u1", Map.of(SMS, "111", EMAIL, "a@x.com"));
            BroadcastResult result = engine.broadcast(notification(Set.of(SMS, EMAIL)), List.of(r));
            assertEquals(2, result.successCount());
            // ordered by channel.name(): EMAIL < SMS
            assertEquals(EMAIL, result.results().get(0).channel());
            assertEquals(SMS, result.results().get(1).channel());
            assertEquals(1, ((SuccessResult) result.results().get(0)).attempts());
            assertEquals(1, sms.sendCount());
            assertEquals(1, email.sendCount());
        }
    }

    @Test
    void recipientOrderPreservedAcrossRecipients() {
        FakeChannel email = new FakeChannel(EMAIL);
        try (var engine = NotificationEngine.builder().policy(fanOut()).registerChannel(email).build()) {
            BroadcastResult result = engine.broadcast(notification(Set.of(EMAIL)),
                    List.of(recipient("u2", EMAIL, "b@x.com"), recipient("u1", EMAIL, "a@x.com")));
            assertEquals("u2", result.results().get(0).recipientId());
            assertEquals("u1", result.results().get(1).recipientId());
        }
    }

    @Test
    void unregisteredTargetChannelIsChannelUnavailable() {
        FakeChannel email = new FakeChannel(EMAIL);
        try (var engine = NotificationEngine.builder().policy(fanOut()).registerChannel(email).build()) {
            BroadcastResult result = engine.broadcast(notification(Set.of(PUSH)),
                    List.of(recipient("u1", PUSH, "ignored")));
            assertEquals(1, result.results().size());
            DeliveryResult dr = result.results().get(0);
            assertInstanceOf(FailedResult.class, dr);
            assertEquals(FailReason.CHANNEL_UNAVAILABLE, ((FailedResult) dr).reason());
            assertEquals(0, ((FailedResult) dr).attempts());
        }
    }

    @Test
    void missingOrBlankAddressIsNoAddress() {
        FakeChannel email = new FakeChannel(EMAIL);
        try (var engine = NotificationEngine.builder().policy(fanOut()).registerChannel(email).build()) {
            Recipient noKey = recipient("u1", Map.of());
            Recipient blank = recipient("u2", Map.of(EMAIL, "   "));
            BroadcastResult result = engine.broadcast(notification(Set.of(EMAIL)), List.of(noKey, blank));
            assertEquals(2, result.skippedCount());
            assertEquals(SkipReason.NO_ADDRESS, ((SkippedResult) result.results().get(0)).reason());
            assertEquals(SkipReason.NO_ADDRESS, ((SkippedResult) result.results().get(1)).reason());
            assertEquals(0, email.sendCount());
        }
    }

    @Test
    void unsupportedChannelIsSkipped() {
        FakeChannel email = new FakeChannel(EMAIL);
        NotificationChannel picky = new NotificationChannel() {
            @Override
            public ChannelType type() { return EMAIL; }
            @Override
            public boolean supports(Notification n, Recipient r) { return false; }
            @Override
            public String send(Notification n, Recipient r) { fail("send must not be called when unsupported"); return null; }
        };
        // picky and email share type EMAIL; build() rejects duplicates, so test separately.
        try (var engine = NotificationEngine.builder().policy(fanOut()).registerChannel(picky).build()) {
            BroadcastResult result = engine.broadcast(notification(Set.of(EMAIL)),
                    List.of(recipient("u1", EMAIL, "a@x.com")));
            assertEquals(SkipReason.UNSUPPORTED, ((SkippedResult) result.results().get(0)).reason());
        }
    }

    @Test
    void retrySucceedsAfterFailures() {
        FlakeyChannel flakey = new FlakeyChannel(EMAIL, 2); // fail twice, succeed on 3rd
        try (var engine = NotificationEngine.builder().policy(fanOut()).registerChannel(flakey).build()) {
            BroadcastResult result = engine.broadcast(notification(Set.of(EMAIL)),
                    List.of(recipient("u1", EMAIL, "a@x.com")));
            assertInstanceOf(SuccessResult.class, result.results().get(0));
            assertEquals(3, ((SuccessResult) result.results().get(0)).attempts());
        }
    }

    @Test
    void nonRetryableFailsImmediately() {
        FakeChannel email = new FakeChannel(EMAIL).behave(FakeChannel.Behavior.NON_RETRYABLE);
        try (var engine = NotificationEngine.builder().policy(fanOut()).registerChannel(email).build()) {
            BroadcastResult result = engine.broadcast(notification(Set.of(EMAIL)),
                    List.of(recipient("u1", EMAIL, "a@x.com")));
            FailedResult fr = (FailedResult) result.results().get(0);
            assertEquals(FailReason.NON_RETRYABLE, fr.reason());
            assertEquals(1, fr.attempts());
            assertEquals(1, email.sendCount()); // not retried
        }
    }

    @Test
    void exhaustedRetriesYieldExceptionFailure() {
        FakeChannel email = new FakeChannel(EMAIL).behave(FakeChannel.Behavior.FAIL);
        try (var engine = NotificationEngine.builder().policy(fanOut()).registerChannel(email).build()) {
            BroadcastResult result = engine.broadcast(notification(Set.of(EMAIL)),
                    List.of(recipient("u1", EMAIL, "a@x.com")));
            FailedResult fr = (FailedResult) result.results().get(0);
            assertEquals(FailReason.EXCEPTION, fr.reason());
            assertEquals(3, fr.attempts()); // maxAttempts=3
            assertEquals("java.lang.RuntimeException", fr.errorCode());
        }
    }

    @Test
    void blankMessageIdIsFailure() {
        FakeChannel email = new FakeChannel(EMAIL).behave(FakeChannel.Behavior.BLANK);
        try (var engine = NotificationEngine.builder().policy(fanOut()).registerChannel(email).build()) {
            BroadcastResult result = engine.broadcast(notification(Set.of(EMAIL)),
                    List.of(recipient("u1", EMAIL, "a@x.com")));
            FailedResult fr = (FailedResult) result.results().get(0);
            assertEquals(FailReason.EXCEPTION, fr.reason());
        }
    }

    @Test
    void failoverStopsAtFirstSuccess() {
        FakeChannel email = new FakeChannel(EMAIL); // succeeds first
        FakeChannel sms = new FakeChannel(SMS);
        try (var engine = NotificationEngine.builder().policy(failover(List.of(EMAIL, SMS)))
                .registerChannel(email).registerChannel(sms).build()) {
            Recipient r = recipient("u1", Map.of(EMAIL, "a@x.com", SMS, "111"));
            BroadcastResult result = engine.broadcast(notification(Set.of(EMAIL, SMS)), List.of(r));
            // EMAIL first in order -> success; SMS short-circuited
            assertEquals(EMAIL, result.results().get(0).channel());
            assertInstanceOf(SuccessResult.class, result.results().get(0));
            SkippedResult smsResult = (SkippedResult) result.results().get(1);
            assertEquals(SMS, smsResult.channel());
            assertEquals(SkipReason.FAILOVER_SHORT_CIRCUITED, smsResult.reason());
            assertEquals(0, sms.sendCount());
        }
    }

    @Test
    void failoverFallsThroughToNextOnFailure() {
        FakeChannel email = new FakeChannel(EMAIL).behave(FakeChannel.Behavior.FAIL); // exhausts retries
        FakeChannel sms = new FakeChannel(SMS); // succeeds
        try (var engine = NotificationEngine.builder().policy(failover(List.of(EMAIL, SMS)))
                .registerChannel(email).registerChannel(sms).build()) {
            Recipient r = recipient("u1", Map.of(EMAIL, "a@x.com", SMS, "111"));
            BroadcastResult result = engine.broadcast(notification(Set.of(EMAIL, SMS)), List.of(r));
            assertInstanceOf(FailedResult.class, result.results().get(0));
            assertInstanceOf(SuccessResult.class, result.results().get(1));
        }
    }

    @Test
    void failoverMarksChannelsNotInOrder() {
        FakeChannel email = new FakeChannel(EMAIL);
        FakeChannel push = new FakeChannel(PUSH); // deliverable but not in failoverOrder
        try (var engine = NotificationEngine.builder().policy(failover(List.of(EMAIL)))
                .registerChannel(email).registerChannel(push).build()) {
            Recipient r = recipient("u1", Map.of(EMAIL, "a@x.com", PUSH, "tok"));
            BroadcastResult result = engine.broadcast(notification(Set.of(EMAIL, PUSH)), List.of(r));
            SkippedResult pushResult = result.results().stream()
                    .filter(dr -> dr.channel().equals(PUSH)).findFirst().map(dr -> (SkippedResult) dr).orElseThrow();
            assertEquals(SkipReason.NOT_IN_FAILOVER_ORDER, pushResult.reason());
        }
    }

    @Test
    void emptyTargetChannelsUsesAllRegistered() {
        FakeChannel email = new FakeChannel(EMAIL);
        try (var engine = NotificationEngine.builder().policy(fanOut()).registerChannel(email).build()) {
            BroadcastResult result = engine.broadcast(notification(Set.of()), // empty -> all registered
                    List.of(recipient("u1", EMAIL, "a@x.com")));
            assertEquals(1, result.successCount());
        }
    }

    @Test
    void broadcastResultCounts() {
        FakeChannel email = new FakeChannel(EMAIL);
        FakeChannel sms = new FakeChannel(SMS).behave(FakeChannel.Behavior.FAIL);
        try (var engine = NotificationEngine.builder().policy(fanOut())
                .registerChannel(email).registerChannel(sms).build()) {
            Recipient r = recipient("u1", Map.of(EMAIL, "a@x.com", SMS, "111"));
            BroadcastResult result = engine.broadcast(notification(Set.of(EMAIL, SMS)), List.of(r));
            assertEquals(2, result.results().size());
            assertEquals(1, result.successCount());
            assertEquals(1, result.failures().size());
            assertEquals(2, result.attemptedCount());
        }
    }
}
