package io.rosecloud.system.domain;

/** Domain view of a role. ORM-free; the persistence layer maps to/from {@code sys_role}. */
public record Role(Long id, String code, String name) {
}
