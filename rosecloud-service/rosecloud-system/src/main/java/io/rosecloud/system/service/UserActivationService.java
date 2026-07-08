package io.rosecloud.system.service;

import io.rosecloud.api.user.UserActivationInfo;

public interface UserActivationService {

    UserActivationInfo check(String activateToken);

    UserActivationInfo confirm(String activateToken, String password);

    UserActivationInfo resend(String username);
}
