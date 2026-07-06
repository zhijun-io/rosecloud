package io.rosecloud.system.domain;

import io.rosecloud.api.user.UserAuthInfo;
import io.rosecloud.common.core.model.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for users. Implemented in the infrastructure layer; the
 * service depends only on this interface so persistence stays swappable.
 */
public interface UserRepository {

    Optional<UserAuthInfo> findAuthInfo(String username);

    boolean existsByUsername(String username);

    Long insert(User user, String passwordHash);

    Optional<User> findById(Long id);

    PageResult<User> page(long current, long size, String keyword);

    void deleteById(Long id);

    List<Long> findRoleIdsByUserId(Long userId);

    List<String> findRoleCodesByUserId(Long userId);

    void assignRoles(Long userId, Collection<Long> roleIds);
}
