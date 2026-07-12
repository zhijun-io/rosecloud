package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.api.notice.NoticePublishApi;
import io.rosecloud.api.notice.NoticePublishRequest;
import io.rosecloud.api.notice.NoticeTargetType;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.UserCredentialEntity;
import io.rosecloud.system.persistence.UserCredentialMapper;
import io.rosecloud.system.persistence.UserEntity;
import io.rosecloud.system.persistence.UserMapper;
import io.rosecloud.system.service.UserActivationService;
import io.rosecloud.system.config.UserActivationProperties;
import io.rosecloud.system.service.dto.UserActivationInfo;
import io.rosecloud.system.support.PasswordPolicyValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserActivationServiceImpl implements UserActivationService {

    private static final long DEFAULT_VERSION = 1L;

    private final UserMapper userMapper;
    private final UserCredentialMapper userCredentialMapper;
    private final PasswordEncoder passwordEncoder;
    private final NoticePublishApi noticePublishApi;
    private final long activationTtlHours;
    private final String activationLinkBaseUrl;
    private final long resendCooldownSeconds;
    private final Map<String, Instant> lastResendAt = new ConcurrentHashMap<>();

    // Global (per-instance) resend ceiling as a first line of defence against email bombing
    // and username enumeration. In a multi-instance deployment pair this with a shared
    // counter (e.g. Redis) so the limit is enforced cluster-wide.
    private static final int GLOBAL_MAX_RESENDS = 30;
    private static final long GLOBAL_WINDOW_SECONDS = 60;
    private final Object globalLock = new Object();
    private Instant globalWindowStart = Instant.EPOCH;
    private int globalResendCount = 0;

    public UserActivationServiceImpl(UserMapper userMapper, UserCredentialMapper userCredentialMapper,
                                     PasswordEncoder passwordEncoder, NoticePublishApi noticePublishApi,
                                     UserActivationProperties properties) {
        this.userMapper = userMapper;
        this.userCredentialMapper = userCredentialMapper;
        this.passwordEncoder = passwordEncoder;
        this.noticePublishApi = noticePublishApi;
        this.activationTtlHours = properties.getActivationTtlHours();
        this.activationLinkBaseUrl = properties.getActivationLinkBaseUrl();
        this.resendCooldownSeconds = properties.getResendCooldownSeconds();
    }

    @Override
    public UserActivationInfo check(String activateToken) {
        UserActivationInfo info = findActivationByToken(activateToken)
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
        confirmActivation(info.userId(), passwordEncoder.encode(password), LocalDateTime.now());
        return findActivationByUsername(info.username())
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_ACTIVATION_TOKEN_INVALID));
    }

    @Override
    @Transactional
    public void resend(String username) {
        if (!acquireGlobalResendQuota()) {
            // Global ceiling reached: drop the request silently to avoid signalling the limit.
            return;
        }
        UserActivationInfo info = findActivationByUsername(username).orElse(null);
        if (info == null || info.used()) {
            // Generic success — never reveal whether the account exists or is already activated,
            // which would otherwise let an attacker enumerate valid usernames.
            return;
        }
        Instant now = Instant.now();
        Instant last = lastResendAt.get(username);
        if (last != null && Duration.between(last, now).toSeconds() < resendCooldownSeconds) {
            // Within the cooldown window: ignore the request without re-sending, mitigating email bombing.
            return;
        }
        lastResendAt.put(username, now);
        LocalDateTime sendTime = LocalDateTime.now();
        LocalDateTime expireTime = sendTime.plusHours(activationTtlHours);
        String token = generateToken();
        long nextVersion = info.version() == null ? DEFAULT_VERSION : info.version() + 1;
        saveActivationToken(info.userId(), token, expireTime, sendTime, nextVersion);
        UserActivationInfo updated = findActivationByToken(token).orElse(null);
        if (updated != null) {
            publishActivationNotice(updated);
        }
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private boolean acquireGlobalResendQuota() {
        Instant now = Instant.now();
        synchronized (globalLock) {
            if (Duration.between(globalWindowStart, now).toSeconds() >= GLOBAL_WINDOW_SECONDS) {
                globalWindowStart = now;
                globalResendCount = 0;
            }
            if (globalResendCount >= GLOBAL_MAX_RESENDS) {
                return false;
            }
            globalResendCount++;
            return true;
        }
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

    private Optional<UserActivationInfo> findActivationByToken(String activateToken) {
        if (activateToken == null || activateToken.isBlank()) {
            return Optional.empty();
        }
        UserCredentialEntity credential = userCredentialMapper.selectOne(
                new LambdaQueryWrapper<UserCredentialEntity>().eq(UserCredentialEntity::getActivateToken, activateToken));
        if (credential == null) {
            return Optional.empty();
        }
        UserEntity user = userMapper.selectById(credential.getUserId());
        if (user == null) {
            return Optional.empty();
        }
        return Optional.of(toActivationInfo(user, credential));
    }

    private Optional<UserActivationInfo> findActivationByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .and(wrapper -> wrapper.eq(UserEntity::getEmail, username)
                        .or()
                        .eq(UserEntity::getPhone, username)));
        if (user == null) {
            return Optional.empty();
        }
        UserCredentialEntity credential = credentialByUserId(user.getId());
        return Optional.of(toActivationInfo(user, credential));
    }

    private void confirmActivation(Long userId, String encodedPassword, LocalDateTime usedTime) {
        UserCredentialEntity credential = credentialByUserId(userId);
        if (credential == null) {
            throw new IllegalStateException("Missing user credential for userId=" + userId);
        }
        credential.setPassword(encodedPassword);
        credential.setPasswordChangedTime(usedTime);
        credential.setActivateToken(null);
        credential.setExpireTime(null);
        credential.setUsedTime(usedTime);
        userCredentialMapper.updateById(credential);

        UserEntity user = userMapper.selectById(userId);
        if (user != null) {
            user.setStatus(1);
            userMapper.updateById(user);
        }
    }

    private void saveActivationToken(Long userId, String activateToken, LocalDateTime expireTime,
                                     LocalDateTime sendTime, Long version) {
        UserCredentialEntity credential = credentialByUserId(userId);
        if (credential == null) {
            throw new IllegalStateException("Missing user credential for userId=" + userId);
        }
        credential.setActivateToken(activateToken);
        credential.setExpireTime(expireTime);
        credential.setUsedTime(null);
        credential.setSendTime(sendTime);
        credential.setVersion(version);
        userCredentialMapper.updateById(credential);
    }

    private UserActivationInfo toActivationInfo(UserEntity user, UserCredentialEntity credential) {
        return new UserActivationInfo(user.getId(), loginName(user), user.getTenantId(),
                credential == null ? null : credential.getActivateToken(),
                credential == null ? null : credential.getExpireTime(),
                credential == null ? null : credential.getUsedTime(),
                credential == null ? null : credential.getSendTime(),
                credential == null ? null : credential.getVersion());
    }

    private UserCredentialEntity credentialByUserId(Long userId) {
        return userCredentialMapper.selectOne(
                new LambdaQueryWrapper<UserCredentialEntity>().eq(UserCredentialEntity::getUserId, userId));
    }

    private String loginName(UserEntity user) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            return user.getPhone();
        }
        return null;
    }
}
