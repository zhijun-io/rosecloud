package io.rosecloud.auth.service.impl;

import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.api.session.LoginSessionApi;
import io.rosecloud.api.session.LoginSessionRequest;
import io.rosecloud.auth.domain.AuthUser;
import io.rosecloud.auth.domain.UserRepository;
import io.rosecloud.auth.error.AuthErrorCode;
import io.rosecloud.auth.service.AuthService;
import io.rosecloud.auth.service.dto.LoginRequest;
import io.rosecloud.auth.service.dto.RefreshRequest;
import io.rosecloud.auth.service.dto.TokenResponse;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.starter.security.jwt.InvalidTokenException;
import io.rosecloud.starter.security.jwt.JwtProperties;
import io.rosecloud.starter.security.jwt.JwtTokenCodec;
import io.rosecloud.starter.security.jwt.TokenClaims;
import io.rosecloud.starter.security.jwt.TokenType;
import io.rosecloud.starter.security.jwt.TokenRevocationService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenCodec jwtTokenCodec;
    private final JwtProperties jwtProperties;
    private final LoginLogApi loginLogApi;
    private final TokenRevocationService tokenRevocationService;
    private final LoginSessionApi loginSessionApi;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           JwtTokenCodec jwtTokenCodec, JwtProperties jwtProperties, LoginLogApi loginLogApi,
                           TokenRevocationService tokenRevocationService, LoginSessionApi loginSessionApi) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenCodec = jwtTokenCodec;
        this.jwtProperties = jwtProperties;
        this.loginLogApi = loginLogApi;
        this.tokenRevocationService = tokenRevocationService;
        this.loginSessionApi = loginSessionApi;
    }

    @Override
    public TokenResponse login(LoginRequest request, String ip, String userAgent) {
        try {
            AuthUser user = userRepository.findByUsername(request.username())
                    .orElseThrow(() -> new BizException(AuthErrorCode.BAD_CREDENTIALS));
            if (user.status() == null || user.status() != 1) {
                throw new BizException(AuthErrorCode.ACCOUNT_DISABLED);
            }
            if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
                throw new BizException(AuthErrorCode.BAD_CREDENTIALS);
            }
            CurrentUser currentUser = new CurrentUser(user.userId(), user.username(), user.tenantId(),
                    user.roles());
            TokenResponse token = issue(currentUser);
            recordSession(token.accessToken(), currentUser, ip, userAgent);
            recordLogin(request.username(), true, null, ip, userAgent);
            return token;
        } catch (BizException e) {
            recordLogin(request.username(), false, e.getErrorCode().message(), ip, userAgent);
            throw e;
        }
    }

    @Override
    public TokenResponse refresh(RefreshRequest request) {
        TokenClaims claims;
        try {
            claims = jwtTokenCodec.parse(request.refreshToken());
        } catch (InvalidTokenException e) {
            throw new BizException(AuthErrorCode.INVALID_TOKEN);
        }
        if (claims.type() != TokenType.REFRESH) {
            throw new BizException(AuthErrorCode.INVALID_TOKEN);
        }
        AuthUser user = userRepository.findByUsername(claims.username())
                .orElseThrow(() -> new BizException(AuthErrorCode.INVALID_TOKEN));
        return issue(new CurrentUser(user.userId(), user.username(), user.tenantId(),
                user.roles()));
    }

    /**
     * Revokes the caller's access token (by {@code jti}) until its expiry, so it
     * is rejected by the gateway/monolith filter before it expires. Already
     * invalid/expired tokens are a no-op. Multi-instance revocation needs a shared
     * store (Redis); the default in-memory store suits the monolith.
     */
    @Override
    public void logout(String bearerToken) {
        String token = extractBearer(bearerToken);
        if (token == null) {
            return;
        }
        try {
            TokenClaims claims = jwtTokenCodec.parse(token);
            if (claims.jti() != null) {
                tokenRevocationService.revoke(claims.jti(), claims.expiresAt());
                try {
                    loginSessionApi.logoutByJti(claims.jti());
                } catch (Exception e) {
                    log.warn("failed to mark login session logged out for jti={}", claims.jti(), e);
                }
            }
        } catch (InvalidTokenException ignored) {
            // already invalid/expired: nothing to revoke
        }
    }

    private static String extractBearer(String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        return null;
    }

    private TokenResponse issue(CurrentUser user) {
        String accessToken = jwtTokenCodec.issueAccessToken(user);
        String refreshToken = jwtTokenCodec.issueRefreshToken(user);
        long expiresIn = jwtProperties.getAccessTtl().toSeconds();
        return new TokenResponse(accessToken, refreshToken, expiresIn);
    }

    /** Records the login session (by access-token jti); never lets it fail the login. */
    private void recordSession(String accessToken, CurrentUser user, String ip, String userAgent) {
        try {
            TokenClaims claims = jwtTokenCodec.parse(accessToken);
            LocalDateTime expireTime = claims.expiresAt() == null
                    ? null : LocalDateTime.ofInstant(claims.expiresAt(), ZoneId.systemDefault());
            loginSessionApi.record(new LoginSessionRequest(claims.jti(), user.userId(), user.username(),
                    user.tenantId(), expireTime, ip, userAgent));
        } catch (Exception e) {
            log.warn("failed to record login session for username={}", user.username(), e);
        }
    }

    /** Reports a login attempt to the system service; never lets logging fail the login. */
    private void recordLogin(String username, boolean success, String failReason, String ip, String userAgent) {
        try {
            loginLogApi.record(new LoginLogRequest(username, success, failReason, ip, userAgent));
        } catch (Exception e) {
            log.warn("failed to record login log for username={}", username, e);
        }
    }
}
