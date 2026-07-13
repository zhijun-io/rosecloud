 package io.rosecloud.common.security.event;
 
 import io.rosecloud.common.security.model.SecurityUser;
 
public record LoginSucceededEvent(SecurityUser securityUser, String ip, String userAgent, String deviceId) {

    public LoginSucceededEvent(SecurityUser securityUser, String ip, String userAgent) {
        this(securityUser, ip, userAgent, null);
    }
}
