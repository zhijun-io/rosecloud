package io.rosecloud.system.service.impl;

import lombok.RequiredArgsConstructor;

import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.common.security.model.LoginSession;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.token.JwtPair;
import io.rosecloud.common.security.token.TokenFactory;
import io.rosecloud.starter.security.session.LoginSessionApi;
import io.rosecloud.system.service.NoAuthService;
import io.rosecloud.system.service.UserActivationService;
import io.rosecloud.system.service.UserService;
import io.rosecloud.system.service.dto.ActivationResult;
import io.rosecloud.system.service.dto.UserActivationInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class NoAuthServiceImpl implements NoAuthService {

    private final UserActivationService userActivationService;
    private final UserService userService;
    private final TokenFactory tokenFactory;
    private final LoginSessionApi loginSessionApi;
    private final LoginLogApi loginLogApi;

    @Override
    public UserActivationInfo check(String activateToken) {
        return userActivationService.check(activateToken);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivationResult activate(String activateToken, String password, String ip, String userAgent) {
        UserActivationInfo info = userActivationService.confirm(activateToken, password);
        SecurityUser securityUser = userService.loadByUsername(info.username());
        JwtPair tokenPair = tokenFactory.createTokenPair(securityUser);

        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(tokenFactory.getAccessTokenExpirationSeconds());
        String ua = StringUtils.abbreviate(userAgent, 512);
        loginSessionApi.save(new LoginSession(
                UUID.randomUUID().toString(),
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                securityUser.getUserId(),
                securityUser.getUsername(),
                securityUser.getNickname(),
                ip,
                ua,
                now,
                expireAt));
        loginLogApi.record(new LoginLogRequest(securityUser.getUsername(), true, null, ip, ua));

        return new ActivationResult(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenFactory.getAccessTokenExpirationSeconds());
    }

    @Override
    public void resend(String username) {
        userActivationService.resend(username);
    }
}
