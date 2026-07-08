package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.DictType;
import io.rosecloud.system.domain.DictTypeRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DictTypeRepositoryImpl implements DictTypeRepository {

    private final DictTypeMapper mapper;

    public DictTypeRepositoryImpl(DictTypeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean existsByCode(String code) {
        return mapper.exists(new LambdaQueryWrapper<DictTypeEntity>().eq(DictTypeEntity::getCode, code));
    }

    @Override
    public Long insert(DictType dictType) {
        DictTypeEntity po = toEntity(dictType);
        po.setId(null);
        mapper.insert(po);
        return po.getId();
    }

    @Override
    public void update(DictType dictType) {
        mapper.updateById(toEntity(dictType));
    }

    @Override
    public Optional<DictType> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<DictType> findByCode(String code) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<DictTypeEntity>().eq(DictTypeEntity::getCode, code))).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public PageResult<DictType> page(long current, long size, String keyword) {
        Page<DictTypeEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<DictTypeEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(DictTypeEntity::getCode, keyword).or().like(DictTypeEntity::getName, keyword);
        }
        wrapper.orderByDesc(DictTypeEntity::getCreateTime);
        IPage<DictTypeEntity> result = mapper.selectPage(page, wrapper);
        List<DictType> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private DictType toDomain(DictTypeEntity po) {
        return new DictType(po.getId(), po.getCode(), po.getName(), po.getStatus(), po.getRemark());
    }

    private DictTypeEntity toEntity(DictType d) {
        DictTypeEntity po = new DictTypeEntity();
        po.setId(d.getId());
        po.setCode(d.getCode());
        po.setName(d.getName());
        po.setStatus(d.getStatus());
        po.setRemark(d.getRemark());
        return po;
    }
}
