package io.rosecloud.api.log;

/**
 * Login-attempt record reported by the auth service to the system service for
 * the login audit log. The system stamps the time on receipt.
 */
public record LoginLogRequest(String username, boolean success, String failReason) {
}
