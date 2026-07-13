package io.rosecloud.notification.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChannelTypeTest {

    @Test
    void normalizesNameToUpperCase() {
        assertEquals("EMAIL", new ChannelType("email").name());
        assertEquals("SMS", new ChannelType(" sms ").name());
    }

    @Test
    void equalWhenNormalizedEqual() {
        assertEquals(new ChannelType("email"), new ChannelType("EMAIL"));
        assertEquals(new ChannelType("email").hashCode(), new ChannelType("EMAIL").hashCode());
    }

    @Test
    void rejectsNullName() {
        assertThrows(IllegalArgumentException.class, () -> new ChannelType(null));
    }

    @Test
    void rejectsInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> new ChannelType("email-1"));
        assertThrows(IllegalArgumentException.class, () -> new ChannelType("1ABC"));
        assertThrows(IllegalArgumentException.class, () -> new ChannelType(""));
    }

    @Test
    void allowsUnderscoreAndDigits() {
        assertEquals("DINGTALK_BOT", new ChannelType("dingtalk_bot").name());
        assertEquals("PUSH_2", new ChannelType("push_2").name());
    }
}
