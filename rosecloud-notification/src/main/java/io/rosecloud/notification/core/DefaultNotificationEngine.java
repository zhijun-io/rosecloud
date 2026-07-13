package io.rosecloud.notification.core;

import io.rosecloud.notification.channel.NonRetryableException;
import io.rosecloud.notification.channel.NotificationChannel;
import io.rosecloud.notification.model.BroadcastResult;
import io.rosecloud.notification.model.ChannelType;
import io.rosecloud.notification.model.DeliveryResult;
import io.rosecloud.notification.model.FailReason;
import io.rosecloud.notification.model.FailedResult;
import io.rosecloud.notification.model.Notification;
import io.rosecloud.notification.model.Recipient;
import io.rosecloud.notification.model.RejectedResult;
import io.rosecloud.notification.model.SkipReason;
import io.rosecloud.notification.model.SkippedResult;
import io.rosecloud.notification.model.SuccessResult;
import io.rosecloud.notification.model.TimeoutResult;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

final class DefaultNotificationEngine implements NotificationEngine {

    private static final Logger log = Logger.getLogger(DefaultNotificationEngine.class.getName());

    private final DeliveryPolicy policy;
    private final Map<ChannelType, NotificationChannel> channels;
    private final ExecutorService executor;
    private final boolean ownsExecutor;
    private final Semaphore permits;
    private volatile boolean closed;

    DefaultNotificationEngine(DeliveryPolicy policy, Map<ChannelType, NotificationChannel> channels,
                              ExecutorService executor, boolean ownsExecutor) {
        this.policy = policy;
        this.channels = channels;
        this.executor = executor;
        this.ownsExecutor = ownsExecutor;
        this.permits = new Semaphore(policy.maxConcurrency());
    }

    @Override
    public BroadcastResult broadcast(Notification n, List<Recipient> recipients) {
        if (closed) {
            throw new IllegalStateException("engine is closed");
        }
        if (n == null) {
            throw new IllegalArgumentException("n must not be null");
        }
        if (recipients == null) {
            throw new IllegalArgumentException("recipients must not be null");
        }
        for (Recipient r : recipients) {
            if (r == null) {
                throw new IllegalArgumentException("recipients must not contain null elements");
            }
        }
        Set<String> seenIds = new HashSet<>();
        for (Recipient r : recipients) {
            if (!seenIds.add(r.recipientId())) {
                throw new IllegalArgumentException("duplicate recipientId within one broadcast: " + r.recipientId());
            }
        }

        long broadcastStart = System.nanoTime();
        Instant deadline = Instant.now().plus(policy.deadline());

        if (recipients.isEmpty()) {
            return new BroadcastResult(n.notificationId(), List.of(), elapsedSince(broadcastStart));
        }

        List<DeliveryResult> all = new ArrayList<>();
        // Recipients are processed concurrently (see design §9.1). Each recipient task
        // resolves all its target channels internally, so it always returns a complete list.
        List<Future<List<DeliveryResult>>> recipientFutures = new ArrayList<>(recipients.size());
        for (Recipient r : recipients) {
            recipientFutures.add(executor.submit(() -> deliverForRecipient(n, r, deadline)));
        }
        for (int i = 0; i < recipients.size(); i++) {
            Recipient r = recipients.get(i);
            Future<List<DeliveryResult>> f = recipientFutures.get(i);
            Duration remaining = Duration.between(Instant.now(), deadline);
            if (remaining.isZero() || remaining.isNegative()) {
                f.cancel(true);
                all.addAll(timeoutAll(n, r));
                continue;
            }
            try {
                all.addAll(f.get(remaining.toMillis(), TimeUnit.MILLISECONDS));
            } catch (TimeoutException te) {
                f.cancel(true);
                all.addAll(timeoutAll(n, r));
            } catch (CancellationException ce) {
                all.addAll(timeoutAll(n, r));
            } catch (ExecutionException ee) {
                Throwable c = ee.getCause() != null ? ee.getCause() : ee;
                log.log(Level.WARNING, "recipient task terminated exceptionally: " + r.recipientId(), c);
                all.addAll(timeoutAll(n, r));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                f.cancel(true);
                all.addAll(timeoutAll(n, r));
            }
        }
        return new BroadcastResult(n.notificationId(), all, elapsedSince(broadcastStart));
    }

    private List<DeliveryResult> deliverForRecipient(Notification n, Recipient r, Instant deadline) {
        long taskStart = System.nanoTime();
        Set<ChannelType> target = n.targetChannels().isEmpty()
                ? channels.keySet() : n.targetChannels();

        List<DeliveryResult> results = new ArrayList<>();
        List<ChannelType> deliverable = new ArrayList<>();
        // Classify each target channel.
        for (ChannelType ct : target) {
            NotificationChannel ch = channels.get(ct);
            if (ch == null) {
                results.add(new FailedResult(n.notificationId(), r.recipientId(), ct,
                        FailReason.CHANNEL_UNAVAILABLE, 0, elapsedSince(taskStart), null, null));
                continue;
            }
            String addr = r.addresses().get(ct);
            if (addr == null || addr.isBlank()) {
                results.add(new SkippedResult(n.notificationId(), r.recipientId(), ct, SkipReason.NO_ADDRESS));
                continue;
            }
            boolean supported;
            try {
                supported = ch.supports(n, r);
            } catch (Exception e) {
                log.log(Level.WARNING, "supports() threw for channel " + ct + " on recipient " + r.recipientId(), e);
                results.add(new FailedResult(n.notificationId(), r.recipientId(), ct,
                        FailReason.EXCEPTION, 0, elapsedSince(taskStart), e.getClass().getName(), e.getMessage()));
                continue;
            }
            if (!supported) {
                results.add(new SkippedResult(n.notificationId(), r.recipientId(), ct, SkipReason.UNSUPPORTED));
                continue;
            }
            deliverable.add(ct);
        }

        if (policy.mode() == DeliveryPolicy.Mode.FAN_OUT) {
            deliverFanOut(n, r, deadline, taskStart, deliverable, results);
        } else {
            deliverFailover(n, r, deadline, taskStart, deliverable, results);
        }

        results.sort(Comparator.comparing(dr -> dr.channel().name()));
        return results;
    }

    private void deliverFanOut(Notification n, Recipient r, Instant deadline, long taskStart,
                               List<ChannelType> deliverable, List<DeliveryResult> results) {
        List<TaskStats> stats = new ArrayList<>(deliverable.size());
        List<Future<DeliveryResult>> futures = new ArrayList<>(deliverable.size());
        for (ChannelType ct : deliverable) {
            TaskStats st = new TaskStats(taskStart);
            stats.add(st);
            NotificationChannel ch = channels.get(ct);
            futures.add(executor.submit(() -> sendWithRetry(n, r, ch, deadline, st)));
        }
        for (int i = 0; i < deliverable.size(); i++) {
            results.add(collect(futures.get(i), stats.get(i), n, r, deliverable.get(i), deadline));
        }
    }

    private void deliverFailover(Notification n, Recipient r, Instant deadline, long taskStart,
                                 List<ChannelType> deliverable, List<DeliveryResult> results) {
        Set<ChannelType> deliverableSet = new HashSet<>(deliverable);
        List<ChannelType> candidates = new ArrayList<>();
        for (ChannelType ct : policy.failoverOrder()) {
            if (deliverableSet.contains(ct)) {
                candidates.add(ct);
            }
        }
        boolean succeeded = false;
        for (ChannelType ct : candidates) {
            if (succeeded) {
                results.add(new SkippedResult(n.notificationId(), r.recipientId(), ct,
                        SkipReason.FAILOVER_SHORT_CIRCUITED));
                continue;
            }
            TaskStats st = new TaskStats(taskStart);
            NotificationChannel ch = channels.get(ct);
            Future<DeliveryResult> f = executor.submit(() -> sendWithRetry(n, r, ch, deadline, st));
            DeliveryResult res = collect(f, st, n, r, ct, deadline);
            results.add(res);
            if (res.isSuccess()) {
                succeeded = true;
            }
        }
        Set<ChannelType> candidateSet = new LinkedHashSet<>(candidates);
        for (ChannelType ct : deliverable) {
            if (!candidateSet.contains(ct)) {
                results.add(new SkippedResult(n.notificationId(), r.recipientId(), ct,
                        SkipReason.NOT_IN_FAILOVER_ORDER));
            }
        }
    }

    private DeliveryResult sendWithRetry(Notification n, Recipient r, NotificationChannel channel,
                                         Instant deadline, TaskStats st) {
        st.taskStartNanos.compareAndSet(0, System.nanoTime());
        Duration backoff = policy.baseBackoff();
        while (true) {
            Duration remaining = Duration.between(Instant.now(), deadline);
            if (remaining.isZero() || remaining.isNegative()) {
                return new TimeoutResult(n.notificationId(), r.recipientId(), channel.type(),
                        st.attempts.get(), st.elapsed());
            }
            Duration wait = remaining.compareTo(policy.acquireTimeout()) <= 0 ? remaining : policy.acquireTimeout();
            boolean acquired;
            try {
                acquired = permits.tryAcquire(wait.toNanos(), TimeUnit.NANOSECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return new TimeoutResult(n.notificationId(), r.recipientId(), channel.type(),
                        st.attempts.get(), st.elapsed());
            }
            if (!acquired) {
                Duration stillRemaining = Duration.between(Instant.now(), deadline);
                if (stillRemaining.isZero() || stillRemaining.isNegative()) {
                    return new TimeoutResult(n.notificationId(), r.recipientId(), channel.type(),
                            st.attempts.get(), st.elapsed());
                }
                return new RejectedResult(n.notificationId(), r.recipientId(), channel.type());
            }
            try {
                st.attempts.incrementAndGet();
                String messageId;
                try {
                    messageId = doSend(channel, n, r);
                } catch (NonRetryableException nre) {
                    log.log(Level.WARNING, "non-retryable send failure for channel "
                            + channel.type() + " recipient " + r.recipientId(), nre);
                    return new FailedResult(n.notificationId(), r.recipientId(), channel.type(),
                            FailReason.NON_RETRYABLE, st.attempts.get(), st.elapsed(),
                            nre.getClass().getName(), nre.getMessage());
                } catch (RuntimeException e) {
                    log.log(Level.WARNING, "send failure for channel "
                            + channel.type() + " recipient " + r.recipientId(), e);
                    if (st.attempts.get() >= policy.maxAttempts()) {
                        return new FailedResult(n.notificationId(), r.recipientId(), channel.type(),
                                FailReason.EXCEPTION, st.attempts.get(), st.elapsed(),
                                e.getClass().getName(), e.getMessage());
                    }
                    Duration rem = Duration.between(Instant.now(), deadline);
                    if (rem.compareTo(backoff) <= 0) {
                        return new TimeoutResult(n.notificationId(), r.recipientId(), channel.type(),
                                st.attempts.get(), st.elapsed());
                    }
                    try {
                        Thread.sleep(backoff.toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return new TimeoutResult(n.notificationId(), r.recipientId(), channel.type(),
                                st.attempts.get(), st.elapsed());
                    }
                    backoff = backoff.multipliedBy(2);
                    continue;
                }
                return new SuccessResult(n.notificationId(), r.recipientId(), channel.type(),
                        messageId, st.attempts.get(), st.elapsed());
            } finally {
                permits.release();
            }
        }
    }

    private String doSend(NotificationChannel channel, Notification n, Recipient r) {
        String id = channel.send(n, r);
        if (id != null && id.isBlank()) {
            throw new IllegalStateException("channel " + channel.type() + " returned blank message id");
        }
        return id;
    }

    private DeliveryResult collect(Future<DeliveryResult> f, TaskStats st, Notification n, Recipient r,
                                   ChannelType ct, Instant deadline) {
        Duration remaining = Duration.between(Instant.now(), deadline);
        if (remaining.isZero() || remaining.isNegative()) {
            f.cancel(true);
            return new TimeoutResult(n.notificationId(), r.recipientId(), ct, st.attempts.get(), st.elapsed());
        }
        try {
            return f.get(remaining.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            f.cancel(true);
            return new TimeoutResult(n.notificationId(), r.recipientId(), ct, st.attempts.get(), st.elapsed());
        } catch (CancellationException ce) {
            return new TimeoutResult(n.notificationId(), r.recipientId(), ct, st.attempts.get(), st.elapsed());
        } catch (ExecutionException ee) {
            Throwable c = ee.getCause() != null ? ee.getCause() : ee;
            return new FailedResult(n.notificationId(), r.recipientId(), ct, FailReason.EXCEPTION,
                    st.attempts.get(), st.elapsed(), c.getClass().getName(), c.getMessage());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            f.cancel(true);
            return new TimeoutResult(n.notificationId(), r.recipientId(), ct, st.attempts.get(), st.elapsed());
        }
    }

    private List<DeliveryResult> timeoutAll(Notification n, Recipient r) {
        Set<ChannelType> target = n.targetChannels().isEmpty()
                ? channels.keySet() : n.targetChannels();
        List<DeliveryResult> out = new ArrayList<>(target.size());
        for (ChannelType ct : target) {
            out.add(new TimeoutResult(n.notificationId(), r.recipientId(), ct, 0, Duration.ZERO));
        }
        out.sort(Comparator.comparing(dr -> dr.channel().name()));
        return out;
    }

    private static Duration elapsedSince(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (ownsExecutor) {
            executor.shutdown();
        }
    }

    private static final class TaskStats {
        final AtomicInteger attempts = new AtomicInteger();
        final AtomicLong taskStartNanos;

        TaskStats(long fallbackStart) {
            this.taskStartNanos = new AtomicLong(fallbackStart);
        }

        Duration elapsed() {
            long start = taskStartNanos.get();
            return Duration.ofNanos(System.nanoTime() - start);
        }
    }
}
