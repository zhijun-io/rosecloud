package io.rosecloud.system.domain;

import io.rosecloud.api.user.UserAuthInfo;
import io.rosecloud.api.user.UserActivationInfo;
import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.common.core.model.PageResult;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for users. Implemented in the infrastructure layer; the
 * service depends only on this interface so persistence stays swappable.
 */
public interface UserRepository {

    Optional<UserAuthInfo> findAuthInfo(String username);

    Optional<UserActivationInfo> findActivationByToken(String activateToken);

    Optional<UserActivationInfo> findActivationByUsername(String username);

    boolean existsByUsername(String username);

    Long insert(User user, String passwordHash);

    void saveActivationToken(Long userId, String activateToken, LocalDateTime expireTime,
                             LocalDateTime sendTime, Long version);

    void confirmActivation(Long userId, String encodedPassword, LocalDateTime usedTime);

    void updateLastLoginTime(Long userId, LocalDateTime lastLoginTime);

    void updatePassword(Long userId, String passwordHash, LocalDateTime passwordChangedTime);

    Optional<User> findById(Long id);

    PageResult<User> page(long current, long size, String keyword);

    void deleteById(Long id);

    List<Long> findRoleIdsByUserId(Long userId);

    List<String> findRoleCodesByUserId(Long userId);

    void assignRoles(Long userId, Collection<Long> roleIds);

    /** Resolves recipient contacts (email/phone) for a notice target. */
    List<NoticeRecipient> findContacts(Integer targetType, String tenantId, String roleCode, String username);
}
