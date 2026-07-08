package io.rosecloud.system.service;

import io.rosecloud.api.user.UserAuthInfo;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.service.dto.UserCreateRequest;
import io.rosecloud.system.service.dto.UserProfile;

import java.util.List;

import java.util.Optional;

public interface UserService {

    Long create(UserCreateRequest request);

    Long createWithHash(String username, String passwordHash, String nickname, String tenantId);

    PageResult<User> page(long current, long size, String keyword);

    User get(Long id);

    void delete(Long id);

    Optional<UserAuthInfo> findAuthInfo(String username);

    void assignRoles(Long userId, List<Long> roleIds);

    List<Long> findRoleIdsByUserId(Long userId);

    UserProfile me();
}
