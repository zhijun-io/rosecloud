package io.rosecloud.system.controller;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.system.domain.DictType;
import io.rosecloud.system.service.DictTypeService;
import io.rosecloud.system.service.dto.DictTypeRequest;
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

@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/dict-types")
public class DictTypeController {

    private final DictTypeService dictTypeService;

    public DictTypeController(DictTypeService dictTypeService) {
        this.dictTypeService = dictTypeService;
    }

    @PreAuthorize("hasAuthority('system:dict:add')")
    @PostMapping
    public ApiResponse<Long> create(@RequestBody DictTypeRequest request) {
        return ApiResponse.ok(dictTypeService.create(request));
    }

    @PreAuthorize("hasAuthority('system:dict:edit')")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody DictTypeRequest request) {
        dictTypeService.update(id, request);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:dict:del')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dictTypeService.delete(id);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:dict:list')")
    @GetMapping("/{id}")
    public ApiResponse<DictType> get(@PathVariable Long id) {
        return ApiResponse.ok(dictTypeService.get(id));
    }

    @PreAuthorize("hasAuthority('system:dict:list')")
    @GetMapping
    public ApiResponse<PagedData<DictType>> page(PageQuery pageQuery) {
        return ApiResponse.ok(dictTypeService.page(pageQuery));
    }
}
