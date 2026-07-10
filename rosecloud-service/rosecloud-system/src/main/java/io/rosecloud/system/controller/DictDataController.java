package io.rosecloud.system.controller;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.system.support.PageSupport;
import io.rosecloud.system.domain.DictData;
import io.rosecloud.system.service.DictDataService;
import io.rosecloud.system.service.dto.DictDataRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/system/dict-data")
public class DictDataController {

    private final DictDataService dictDataService;

    public DictDataController(DictDataService dictDataService) {
        this.dictDataService = dictDataService;
    }

    @PreAuthorize("hasAuthority('system:dict:add')")
    @PostMapping
    public ApiResponse<Long> create(@RequestBody DictDataRequest request) {
        return ApiResponse.ok(dictDataService.create(request));
    }

    @PreAuthorize("hasAuthority('system:dict:edit')")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody DictDataRequest request) {
        dictDataService.update(id, request);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:dict:del')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dictDataService.delete(id);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:dict:list')")
    @GetMapping("/{id}")
    public ApiResponse<DictData> get(@PathVariable Long id) {
        return ApiResponse.ok(dictDataService.get(id));
    }

    /** Enabled items for a dictionary code (frontend dropdowns). */
    @PreAuthorize("hasAuthority('system:dict:list')")
    @GetMapping("/by-code/{dictCode}")
    public ApiResponse<List<DictData>> listByCode(@PathVariable String dictCode) {
        return ApiResponse.ok(dictDataService.listByCode(dictCode));
    }

    @PreAuthorize("hasAuthority('system:dict:list')")
    @GetMapping
    public ApiResponse<PageResult<DictData>> page(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String dictCode) {
        return ApiResponse.ok(dictDataService.page(PageSupport.current(current), PageSupport.size(size), dictCode));
    }
}
