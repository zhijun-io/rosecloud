 package io.rosecloud.common.security.event;
 
 public record LoginFailedEvent(String username, String tenantCode, String reason,
                                 String ip, String userAgent) {
 }
