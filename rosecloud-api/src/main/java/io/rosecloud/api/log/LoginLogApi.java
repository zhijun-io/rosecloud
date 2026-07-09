package io.rosecloud.api.log;

/**
 * Service contract for recording login attempts in the system service's login
 * audit log. Transport-specific annotations live on {@link LoginLogFeignApi}.
 */
public interface LoginLogApi {

    void record(LoginLogRequest request);
}
