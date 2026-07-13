package io.rosecloud.api.log;

/**
 * Login-attempt record reported to the auth service for the login audit log.
 * The auth service stamps the time on receipt.
 */
public record LoginLogRequest(String username, boolean success, String failReason,
                               String ip, String userAgent, String deviceId) {

    public LoginLogRequest(String username, boolean success, String failReason, String ip, String userAgent) {
        this(username, success, failReason, ip, userAgent, null);
    }
}
