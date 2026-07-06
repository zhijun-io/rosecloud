package io.rosecloud.auth.domain;

/**
 * Auth-domain view of a user. ORM-free so the persistence flavor (MyBatis-Plus
 * today, JPA later) can change behind the repository port.
 */
public record AuthUser(Long userId, String username, String passwordHash, Integer status, Long tenantId) {
}
