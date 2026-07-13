package io.rosecloud.auth.service.impl;

import io.rosecloud.auth.persistence.LoginSessionEntity;
import io.rosecloud.auth.persistence.LoginSessionMapper;
import io.rosecloud.common.security.model.LoginSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginSessionServiceImplTest {

    @Mock
    LoginSessionMapper sessionMapper;

    private LoginSessionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LoginSessionServiceImpl(sessionMapper);
    }

    private void setLimit(int n) {
        ReflectionTestUtils.setField(service, "maxConcurrentSessions", n);
    }

    private LoginSession session(String id, String token) {
        Instant now = Instant.now();
        return new LoginSession(id, token, token + "-r", 1L, "alice", "Alice",
                "10.0.0.1", "JUnit", now, now.plusSeconds(3600), "device-1");
    }

    private LoginSessionEntity entity(String id, String token) {
        LoginSessionEntity e = new LoginSessionEntity();
        e.setSessionId(id);
        e.setToken(token);
        e.setUserId(1L);
        e.setUsername("alice");
        e.setDeviceId("device-1");
        return e;
    }

    @Test
    void savePersistsToDb() {
        setLimit(100);
        when(sessionMapper.selectList(any())).thenReturn(List.of());
        LoginSession s = session("s-new", "tok-new");

        service.save(s);

        verify(sessionMapper).insert(any(LoginSessionEntity.class));
    }

    @Test
    void saveEnforcesConcurrentLimitByRevokingOldest() {
        setLimit(2);
        LoginSessionEntity older = entity("s-old", "tok-old");
        LoginSessionEntity newer = entity("s-new", "tok-new");
        when(sessionMapper.selectList(any())).thenReturn(List.of(older, newer));

        service.save(session("s-third", "tok-third"));

        verify(sessionMapper).insert(any(LoginSessionEntity.class));
        verify(sessionMapper).update(any(LoginSessionEntity.class), any());
    }

    @Test
    void revokeByTokenMarksDb() {
        service.revoke("tok-x");

        verify(sessionMapper).update(any(LoginSessionEntity.class), any());
    }

    @Test
    void isRevokedReadsDb() {
        when(sessionMapper.selectCount(any())).thenReturn(1L);
        assertTrue(service.isRevoked("tok-revoked"));

        when(sessionMapper.selectCount(any())).thenReturn(0L);
        assertFalse(service.isRevoked("tok-fresh"));
    }

    @Test
    void findBySessionIdMapsDeviceId() {
        LoginSessionEntity e = entity("s-x", "tok-x");
        when(sessionMapper.selectOne(any())).thenReturn(e);

        LoginSession found = service.findBySessionId("s-x").orElse(null);

        assertNotNull(found);
        assertEquals("s-x", found.id());
        assertEquals("device-1", found.deviceId());
    }
}
