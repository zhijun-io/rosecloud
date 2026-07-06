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
        DictDataPO po = toPO(dictData);
        po.setId(null);
        mapper.insert(po);
        return po.getId();
    }

    @Override
    public void update(DictData dictData) {
        mapper.updateById(toPO(dictData));
    }

    @Override
    public Optional<DictData> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<DictData> findByDictCode(String dictCode) {
        return mapper.selectList(new LambdaQueryWrapper<DictDataPO>()
                .eq(DictDataPO::getDictCode, dictCode)
                .orderByAsc(DictDataPO::getSort)).stream().map(this::toDomain).toList();
    }

    @Override
    public List<DictData> findEnabledByDictCode(String dictCode) {
        return mapper.selectList(new LambdaQueryWrapper<DictDataPO>()
                .eq(DictDataPO::getDictCode, dictCode)
                .eq(DictDataPO::getStatus, 1)
                .orderByAsc(DictDataPO::getSort)).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByDictCode(String dictCode) {
        mapper.delete(new LambdaQueryWrapper<DictDataPO>().eq(DictDataPO::getDictCode, dictCode));
    }

    @Override
    public PageResult<DictData> page(long current, long size, String dictCode) {
        Page<DictDataPO> page = new Page<>(current, size);
        LambdaQueryWrapper<DictDataPO> wrapper = new LambdaQueryWrapper<>();
        if (dictCode != null && !dictCode.isBlank()) {
            wrapper.eq(DictDataPO::getDictCode, dictCode);
        }
        wrapper.orderByAsc(DictDataPO::getSort).orderByDesc(DictDataPO::getCreateTime);
        IPage<DictDataPO> result = mapper.selectPage(page, wrapper);
        List<DictData> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private DictData toDomain(DictDataPO po) {
        return new DictData(po.getId(), po.getDictCode(), po.getLabel(), po.getValue(),
                po.getSort(), po.getStatus(), po.getRemark());
    }

    private DictDataPO toPO(DictData d) {
        DictDataPO po = new DictDataPO();
        po.setId(d.id());
        po.setDictCode(d.dictCode());
        po.setLabel(d.label());
        po.setValue(d.value());
        po.setSort(d.sort());
        po.setStatus(d.status());
        po.setRemark(d.remark());
        return po;
    }
}
