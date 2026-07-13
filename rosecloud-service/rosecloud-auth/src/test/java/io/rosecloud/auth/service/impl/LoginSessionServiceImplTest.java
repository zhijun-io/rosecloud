package io.rosecloud.auth.service.impl;

import io.rosecloud.auth.persistence.LoginSessionEntity;
import io.rosecloud.auth.persistence.LoginSessionMapper;
import io.rosecloud.common.security.model.LoginSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginSessionServiceImplTest {

    @Mock
    StringRedisTemplate redisTemplate;
    @Mock
    LoginSessionMapper sessionMapper;
    @Mock
    HashOperations<String, Object, Object> hashOps;
    @Mock
    SetOperations<String, String> setOps;
    @Mock
    ValueOperations<String, String> valueOps;

    private LoginSessionServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOps);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOps);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new LoginSessionServiceImpl(redisTemplate, sessionMapper);
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

    private String fakeToken(String jti) {
        String payload = "{\"jti\":\"" + jti + "\",\"exp\":4102444800}";
        String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        return "header." + b64 + ".sig";
    }

    @Test
    void saveDualWritesDbAndRedis() {
        setLimit(100);
        when(sessionMapper.selectList(any())).thenReturn(List.of());
        LoginSession s = session("s-new", "tok-new");

        service.save(s);

        verify(sessionMapper).insert(any(LoginSessionEntity.class));
        verify(hashOps).putAll(eq("session:s-new"), any());
        verify(setOps).add(eq("session:ids"), eq("s-new"));
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
    void revokeByTokenMarksDbAndAddsRevocationKey() {
        service.revoke(fakeToken("jti-1"));

        verify(sessionMapper).update(any(LoginSessionEntity.class), any());
        verify(valueOps).set(eq("revoked:jti-1"), eq(""), any(Duration.class));
    }

    @Test
    void isRevokedReadsRevocationSet() {
        when(redisTemplate.hasKey("revoked:jti-2")).thenReturn(true);
        assertTrue(service.isRevoked(fakeToken("jti-2")));

        when(redisTemplate.hasKey("revoked:jti-3")).thenReturn(false);
        assertFalse(service.isRevoked(fakeToken("jti-3")));
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
