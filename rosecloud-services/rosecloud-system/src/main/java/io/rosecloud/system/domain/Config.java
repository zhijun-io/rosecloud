package io.rosecloud.system.domain;

/** Domain view of a business configuration entry. ORM-free; mapped to/from {@code sys_config}. */
public record Config(Long id, String configKey, String configValue, String description) {
}
