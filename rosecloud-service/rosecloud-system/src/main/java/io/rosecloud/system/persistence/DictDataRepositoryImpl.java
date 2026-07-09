package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.DictData;
import io.rosecloud.system.domain.DictDataRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DictDataRepositoryImpl implements DictDataRepository {

    private final DictDataMapper mapper;

    public DictDataRepositoryImpl(DictDataMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Long insert(DictData dictData) {
        DictDataEntity po = toEntity(dictData);
        po.setId(null);
        mapper.insert(po);
        return po.getId();
    }

    @Override
    public void update(DictData dictData) {
        mapper.updateById(toEntity(dictData));
    }

    @Override
    public Optional<DictData> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<DictData> findByDictCode(String dictCode) {
        return mapper.selectList(new LambdaQueryWrapper<DictDataEntity>()
                .eq(DictDataEntity::getDictCode, dictCode)
                .orderByAsc(DictDataEntity::getSort)).stream().map(this::toDomain).toList();
    }

    @Override
    public List<DictData> findEnabledByDictCode(String dictCode) {
        return mapper.selectList(new LambdaQueryWrapper<DictDataEntity>()
                .eq(DictDataEntity::getDictCode, dictCode)
                .eq(DictDataEntity::getStatus, 1)
                .orderByAsc(DictDataEntity::getSort)).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByDictCode(String dictCode) {
        mapper.delete(new LambdaQueryWrapper<DictDataEntity>().eq(DictDataEntity::getDictCode, dictCode));
    }

    @Override
    public PageResult<DictData> page(long current, long size, String dictCode) {
        Page<DictDataEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<DictDataEntity> wrapper = new LambdaQueryWrapper<>();
        if (dictCode != null && !dictCode.isBlank()) {
            wrapper.eq(DictDataEntity::getDictCode, dictCode);
        }
        wrapper.orderByAsc(DictDataEntity::getSort).orderByDesc(DictDataEntity::getCreateTime);
        IPage<DictDataEntity> result = mapper.selectPage(page, wrapper);
        List<DictData> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private DictData toDomain(DictDataEntity po) {
        return new DictData(po.getId(), po.getDictCode(), po.getLabel(), po.getValue(),
                po.getSort(), po.getStatus(), po.getRemark(), po.getCreateTime(), po.getCreateBy(),
                po.getUpdateTime(), po.getUpdateBy());
    }

    private DictDataEntity toEntity(DictData d) {
        DictDataEntity po = new DictDataEntity();
        po.setId(d.getId());
        po.setDictCode(d.getDictCode());
        po.setLabel(d.getLabel());
        po.setValue(d.getValue());
        po.setSort(d.getSort());
        po.setStatus(d.getStatus());
        po.setRemark(d.getRemark());
        po.setCreateTime(d.getCreateTime());
        po.setCreateBy(d.getCreateBy());
        po.setUpdateTime(d.getUpdateTime());
        po.setUpdateBy(d.getUpdateBy());
        return po;
    }
}
