package io.rosecloud.notification.channel;

/**
 * Thrown by a channel to signal a non-retryable failure. The engine does not
 * retry when this is raised; any other exception is retried per policy.
 */
public class NonRetryableException extends RuntimeException {
    public NonRetryableException(String message) { super(message); }
    public NonRetryableException(String message, Throwable cause) { super(message, cause); }
}
