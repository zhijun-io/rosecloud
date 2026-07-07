package io.rosecloud.system.domain;

/** Domain view of a user. ORM-free; the persistence layer maps to/from {@code sys_user}. */
public record User(Long id, String username, String nickname, Integer status, Long tenantId) {
}
