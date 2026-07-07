package io.rosecloud.auth.service.impl;

import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.session.LoginSessionApi;
import io.rosecloud.auth.config.LoginProtectionProperties;
import io.rosecloud.auth.domain.UserRepository;
import io.rosecloud.auth.domain.AuthUser;
import io.rosecloud.auth.service.dto.LoginRequest;
import io.rosecloud.auth.service.dto.TokenResponse;
import io.rosecloud.auth.service.security.LoginProtectionService;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.starter.cache.LocalRoseCloudCache;
import io.rosecloud.starter.cache.RoseCloudCache;
import io.rosecloud.starter.security.SecurityErrorCode;
import io.rosecloud.starter.security.jwt.JwtProperties;
import io.rosecloud.starter.security.jwt.JwtTokenCodec;
import io.rosecloud.starter.security.jwt.TokenClaims;
import io.rosecloud.starter.security.jwt.TokenRevocationService;
import io.rosecloud.starter.security.jwt.TokenType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenCodec jwtTokenCodec;
    @Mock JwtProperties jwtProperties;
    @Mock LoginLogApi loginLogApi;
    @Mock TokenRevocationService tokenRevocationService;
    @Mock LoginSessionApi loginSessionApi;

    private AuthServiceImpl service(RoseCloudCache cache) {
        LoginProtectionService protection = new LoginProtectionService(cache, new LoginProtectionProperties());
        return new AuthServiceImpl(userRepository, passwordEncoder, jwtTokenCodec, jwtProperties,
                loginLogApi, tokenRevocationService, loginSessionApi, protection);
    }

    private void stubActiveUser(String username, String hash) {
        when(userRepository.findByUsername(username)).thenReturn(
                Optional.of(new AuthUser(1L, username, hash, 1, 1L, List.of("ROLE"), List.of())));
    }

    @Test
    void locksAccountAfterMaxFailedAttempts() {
        RoseCloudCache cache = new LocalRoseCloudCache();
        AuthServiceImpl s = service(cache);
        stubActiveUser("alice", "hash");
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(false);

        for (int i = 0; i < 5; i++) {
            BizException ex = assertThrows(BizException.class,
                    () -> s.login(new LoginRequest("alice", "wrong"), "1.1.1.1", "ua"));
            assertEquals(SecurityErrorCode.BAD_CREDENTIALS, ex.getErrorCode());
        }
        BizException locked = assertThrows(BizException.class,
                () -> s.login(new LoginRequest("alice", "wrong"), "1.1.1.1", "ua"));
        assertEquals(SecurityErrorCode.ACCOUNT_LOCKED, locked.getErrorCode());
        // the locked attempt never reaches the password matcher
        verify(passwordEncoder, times(5)).matches(anyString(), anyString());
    }

    @Test
    void successfulLoginClearsCounters() {
        RoseCloudCache cache = new LocalRoseCloudCache();
        AuthServiceImpl s = service(cache);
        stubActiveUser("alice", "hash");
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenAnswer(inv -> "right".equals(inv.getArgument(0)));
        when(jwtProperties.getAccessTtl()).thenReturn(Duration.ofMinutes(30));
        when(jwtTokenCodec.issueAccessToken(any(CurrentUser.class))).thenReturn("access");
        when(jwtTokenCodec.issueRefreshToken(any(CurrentUser.class))).thenReturn("refresh");
        when(jwtTokenCodec.parse("access")).thenReturn(
                new TokenClaims("alice", TokenType.ACCESS, "jti-1", Instant.now().plusSeconds(1800), List.of()));

        for (int i = 0; i < 2; i++) {
            assertThrows(BizException.class, () -> s.login(new LoginRequest("alice", "bad"), "1.1.1.1", "ua"));
        }
        TokenResponse token = s.login(new LoginRequest("alice", "right"), "1.1.1.1", "ua");
        assertNotNull(token);

        // counters cleared on success: 5 more wrong attempts re-count from zero,
        // the 5th returns BAD_CREDENTIALS (lock set), the 6th is ACCOUNT_LOCKED
        for (int i = 0; i < 5; i++) {
            BizException ex = assertThrows(BizException.class,
                    () -> s.login(new LoginRequest("alice", "bad"), "1.1.1.1", "ua"));
            assertEquals(SecurityErrorCode.BAD_CREDENTIALS, ex.getErrorCode());
        }
        BizException locked = assertThrows(BizException.class,
                () -> s.login(new LoginRequest("alice", "bad"), "1.1.1.1", "ua"));
        assertEquals(SecurityErrorCode.ACCOUNT_LOCKED, locked.getErrorCode());
    }
}
