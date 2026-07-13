package io.rosecloud.system.controller;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.system.domain.SystemSetting;
import io.rosecloud.system.service.SystemSettingService;
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

@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/system-settings")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    public SystemSettingController(SystemSettingService systemSettingService) {
        this.systemSettingService = systemSettingService;
    }

    @PreAuthorize("hasAuthority('system:setting:list')")
    @GetMapping
    public ApiResponse<List<SystemSetting>> list() {
        return ApiResponse.ok(systemSettingService.list());
    }

    @PreAuthorize("hasAuthority('system:setting:list')")
    @GetMapping("/{key}")
    public ApiResponse<SystemSetting> get(@PathVariable String key) {
        return ApiResponse.ok(systemSettingService.get(key));
    }

    @PreAuthorize("hasAuthority('system:setting:edit')")
    @PutMapping("/{key}")
    public ApiResponse<Void> save(@PathVariable String key, @RequestBody SettingValueRequest request) {
        systemSettingService.save(key, request);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:setting:del')")
    @DeleteMapping("/{key}")
    public ApiResponse<Void> delete(@PathVariable String key) {
        systemSettingService.delete(key);
        return ApiResponse.ok();
    }
}
