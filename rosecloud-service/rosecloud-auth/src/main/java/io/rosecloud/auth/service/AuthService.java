package io.rosecloud.auth.service;

import io.rosecloud.auth.service.dto.LoginRequest;
import io.rosecloud.auth.service.dto.RefreshRequest;
import io.rosecloud.auth.service.dto.TokenResponse;

public interface AuthService {

    TokenResponse login(LoginRequest request, String ip, String userAgent);

    TokenResponse refresh(RefreshRequest request);

    void logout(String bearerToken);
}
