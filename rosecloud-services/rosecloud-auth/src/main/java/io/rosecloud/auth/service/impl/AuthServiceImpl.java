package io.rosecloud.auth.service.impl;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenCodec jwtTokenCodec;
    private final JwtProperties jwtProperties;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           JwtTokenCodec jwtTokenCodec, JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenCodec = jwtTokenCodec;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        AuthUser user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BizException(AuthErrorCode.BAD_CREDENTIALS));
        if (user.status() == null || user.status() != 1) {
            throw new BizException(AuthErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new BizException(AuthErrorCode.BAD_CREDENTIALS);
        }
        return issue(new CurrentUser(user.userId(), user.username(), user.tenantId(), user.roles(), null));
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
}
