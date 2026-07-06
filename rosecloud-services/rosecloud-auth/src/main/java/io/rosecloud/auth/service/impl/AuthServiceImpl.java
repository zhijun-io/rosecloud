package io.rosecloud.auth.service.impl;

import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.log.LoginLogRequest;
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

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           JwtTokenCodec jwtTokenCodec, JwtProperties jwtProperties, LoginLogApi loginLogApi) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenCodec = jwtTokenCodec;
        this.jwtProperties = jwtProperties;
        this.loginLogApi = loginLogApi;
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        try {
            AuthUser user = userRepository.findByUsername(request.username())
                    .orElseThrow(() -> new BizException(AuthErrorCode.BAD_CREDENTIALS));
            if (user.status() == null || user.status() != 1) {
                throw new BizException(AuthErrorCode.ACCOUNT_DISABLED);
            }
            if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
                throw new BizException(AuthErrorCode.BAD_CREDENTIALS);
            }
            TokenResponse token = issue(new CurrentUser(user.userId(), user.username(), user.tenantId(),
                    user.roles(), null));
            recordLogin(request.username(), true, null);
            return token;
        } catch (BizException e) {
            recordLogin(request.username(), false, e.getErrorCode().message());
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
        return issue(new CurrentUser(claims.userId(), claims.username(), claims.tenantId(),
                claims.roles(), null));
    }

    /**
     * Stateless logout: clients discard their tokens. Token revocation / blacklist
     * (which needs a token store) is a follow-up.
     */
    @Override
    public void logout() {
    }

    private TokenResponse issue(CurrentUser user) {
        String accessToken = jwtTokenCodec.issueAccessToken(user);
        String refreshToken = jwtTokenCodec.issueRefreshToken(user);
        long expiresIn = jwtProperties.getAccessTtl().toSeconds();
        return new TokenResponse(accessToken, refreshToken, expiresIn);
    }

    /** Reports a login attempt to the system service; never lets logging fail the login. */
    private void recordLogin(String username, boolean success, String failReason) {
        try {
            loginLogApi.record(new LoginLogRequest(username, success, failReason));
        } catch (Exception e) {
            log.warn("failed to record login log for username={}", username, e);
        }
    }
}
