package io.rosecloud.system.domain;

import java.util.Objects;

/** Domain view of a role. ORM-free; the persistence layer maps to/from {@code sys_role}. */
public final class Role {

    private final Long id;
    private final String code;
    private final String name;

    public Role(Long id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        return Objects.equals(id, role.id) && Objects.equals(code, role.code) && Objects.equals(name, role.name);
    }

    @Override
    public int hashCode() { return Objects.hash(id, code, name); }

    @Override
    public String toString() {
        return "Role[" + "id=" + id + ", code=" + code + ", name=" + name + ']';
    }
}
