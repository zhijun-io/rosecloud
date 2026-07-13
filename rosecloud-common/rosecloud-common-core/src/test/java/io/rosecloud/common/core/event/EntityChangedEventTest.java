package io.rosecloud.common.core.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityChangedEventTest {

    @Test
    void createdBuildsValidEvent() {
        EntityChangedEvent<String> event = EntityChangedEvent.created("user", 1L, "ROOT", "after");
        assertEquals("user", event.entityType());
        assertEquals(1L, event.entityId());
        assertEquals(EntityChangeType.CREATED, event.changeType());
        assertEquals("ROOT", event.tenantId());
        assertNull(event.before());
        assertEquals("after", event.after());
    }

    @Test
    void updatedBuildsValidEvent() {
        EntityChangedEvent<String> event = EntityChangedEvent.updated("user", 1L, "ROOT", "before", "after");
        assertEquals(EntityChangeType.UPDATED, event.changeType());
        assertEquals("before", event.before());
        assertEquals("after", event.after());
    }

    @Test
    void deletedBuildsValidEvent() {
        EntityChangedEvent<String> event = EntityChangedEvent.deleted("user", 1L, "ROOT", "before");
        assertEquals(EntityChangeType.DELETED, event.changeType());
        assertEquals("before", event.before());
        assertNull(event.after());
    }

    @Test
    void rejectsNullEntityType() {
        assertThrows(IllegalArgumentException.class,
                () -> new EntityChangedEvent<>(null, 1L, EntityChangeType.CREATED, null, null, null));
    }

    @Test
    void rejectsBlankEntityType() {
        assertThrows(IllegalArgumentException.class,
                () -> new EntityChangedEvent<>("  ", 1L, EntityChangeType.CREATED, null, null, null));
    }

    @Test
    void rejectsNullEntityId() {
        assertThrows(IllegalArgumentException.class,
                () -> new EntityChangedEvent<>("user", null, EntityChangeType.CREATED, null, null, null));
    }

    @Test
    void rejectsNullChangeType() {
        assertThrows(IllegalArgumentException.class,
                () -> new EntityChangedEvent<>("user", 1L, null, null, null, null));
    }
}
