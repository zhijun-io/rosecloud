package io.rosecloud.system.service.impl;

import io.rosecloud.system.service.dto.UserActivationInfo;
import io.rosecloud.api.notice.NoticePublishApi;
import io.rosecloud.api.notice.NoticePublishRequest;
import io.rosecloud.api.notice.NoticeTargetType;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.system.domain.UserRepository;
import io.rosecloud.system.service.UserActivationService;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.support.PasswordPolicyValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserActivationServiceImpl implements UserActivationService {

    private static final long DEFAULT_VERSION = 1L;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NoticePublishApi noticePublishApi;
    private final long activationTtlHours;
    private final String activationLinkBaseUrl;

    public UserActivationServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                     NoticePublishApi noticePublishApi,
                                     @Value("${rosecloud.user.activation-ttl-hours:24}") long activationTtlHours,
                                     @Value("${rosecloud.user.activation-link-base-url:}") String activationLinkBaseUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.noticePublishApi = noticePublishApi;
        this.activationTtlHours = activationTtlHours;
        this.activationLinkBaseUrl = activationLinkBaseUrl;
    }

    @Override
    public UserActivationInfo check(String activateToken) {
        UserActivationInfo info = userRepository.findActivationByToken(activateToken)
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_ACTIVATION_TOKEN_INVALID));
        return info;
    }

    @Override
    @Transactional
    public UserActivationInfo confirm(String activateToken, String password) {
        UserActivationInfo info = check(activateToken);
        if (info.used()) {
            throw new BizException(SystemErrorCode.USER_ACTIVATION_TOKEN_USED);
        }
        if (info.expired()) {
            throw new BizException(SystemErrorCode.USER_ACTIVATION_TOKEN_EXPIRED);
        }
        PasswordPolicyValidator.validate(password);
        userRepository.confirmActivation(info.userId(), passwordEncoder.encode(password), LocalDateTime.now());
        return userRepository.findActivationByUsername(info.username())
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_ACTIVATION_TOKEN_INVALID));
    }

    @Override
    @Transactional
    public UserActivationInfo resend(String username) {
        UserActivationInfo info = userRepository.findActivationByUsername(username)
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_NOT_FOUND));
        if (info.used()) {
            throw new BizException(SystemErrorCode.USER_ACTIVATION_TOKEN_USED);
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusHours(activationTtlHours);
        String token = generateToken();
        long nextVersion = info.version() == null ? DEFAULT_VERSION : info.version() + 1;
        userRepository.saveActivationToken(info.userId(), token, expireTime, now, nextVersion);
        UserActivationInfo updated = userRepository.findActivationByToken(token)
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_NOT_FOUND));
        publishActivationNotice(updated);
        return updated;
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void publishActivationNotice(UserActivationInfo info) {
        String link = activationLink(info.activateToken());
        String content = "请使用以下激活链接完成首次登录并设置密码：\n" + link;
        try {
            noticePublishApi.publish(new NoticePublishRequest("租户管理员激活", content,
                    NoticeTargetType.USER.code(), null, null, info.username(),
                    null, LocalDateTime.now(), null, null, false, 2 | 4, List.of()));
        } catch (Exception ignored) {
            // best effort: token generation and persistence are the source of truth
        }
    }

    private String activationLink(String token) {
        String path = ServiceMetadata.API_PREFIX + "/noauth/activate?activateToken=" + token;
        if (activationLinkBaseUrl == null || activationLinkBaseUrl.isBlank()) {
            return path;
        }
        String base = activationLinkBaseUrl.endsWith("/")
                ? activationLinkBaseUrl.substring(0, activationLinkBaseUrl.length() - 1)
                : activationLinkBaseUrl;
        return base + path;
    }
}
