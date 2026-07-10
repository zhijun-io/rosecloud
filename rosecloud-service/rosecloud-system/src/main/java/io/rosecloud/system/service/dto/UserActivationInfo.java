package io.rosecloud.system.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

public record UserActivationInfo(Long userId, String username, String tenantId,
                                 @JsonIgnore String activateToken,
                                 LocalDateTime expireTime, LocalDateTime usedTime, LocalDateTime sendTime,
                                 Long version) {

    public boolean used() {
        return usedTime != null;
    }

    public boolean expired() {
        return expireTime != null && expireTime.isBefore(LocalDateTime.now());
    }

    public boolean available() {
        return activateToken != null && !used() && !expired();
    }
}
