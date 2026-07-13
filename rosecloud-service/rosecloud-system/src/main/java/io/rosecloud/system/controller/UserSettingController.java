package io.rosecloud.system.controller;
import lombok.RequiredArgsConstructor;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.system.domain.UserSetting;
import io.rosecloud.system.service.UserSettingService;
import io.rosecloud.system.service.dto.SettingValueRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/user-settings")
public class UserSettingController {

    private final UserSettingService userSettingService;
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ApiResponse<List<UserSetting>> listMine() {
        return ApiResponse.ok(userSettingService.listMine());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{key}")
    public ApiResponse<UserSetting> getMine(@PathVariable String key) {
        return ApiResponse.ok(userSettingService.getMine(key));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{key}")
    public ApiResponse<Void> saveMine(@PathVariable String key, @RequestBody SettingValueRequest request) {
        userSettingService.saveMine(key, request);
        return ApiResponse.ok();
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{key}")
    public ApiResponse<Void> deleteMine(@PathVariable String key) {
        userSettingService.deleteMine(key);
        return ApiResponse.ok();
    }
}
