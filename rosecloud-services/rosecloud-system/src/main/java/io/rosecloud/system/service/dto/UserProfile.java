package io.rosecloud.system.service.dto;

import io.rosecloud.system.domain.User;

import java.util.List;

/** Current user's profile plus their role codes. */
public record UserProfile(User user, List<String> roles) {
}
