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
        return mapper.exists(new LambdaQueryWrapper<DictTypePO>().eq(DictTypePO::getCode, code));
    }

    @Override
    public Long insert(DictType dictType) {
        DictTypePO po = toPO(dictType);
        po.setId(null);
        mapper.insert(po);
        return po.getId();
    }

    @Override
    public void update(DictType dictType) {
        mapper.updateById(toPO(dictType));
    }

    @Override
    public Optional<DictType> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<DictType> findByCode(String code) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<DictTypePO>().eq(DictTypePO::getCode, code))).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public PageResult<DictType> page(long current, long size, String keyword) {
        Page<DictTypePO> page = new Page<>(current, size);
        LambdaQueryWrapper<DictTypePO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(DictTypePO::getCode, keyword).or().like(DictTypePO::getName, keyword);
        }
        wrapper.orderByDesc(DictTypePO::getCreateTime);
        IPage<DictTypePO> result = mapper.selectPage(page, wrapper);
        List<DictType> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private DictType toDomain(DictTypePO po) {
        return new DictType(po.getId(), po.getCode(), po.getName(), po.getStatus(), po.getRemark());
    }

    private DictTypePO toPO(DictType d) {
        DictTypePO po = new DictTypePO();
        po.setId(d.id());
        po.setCode(d.code());
        po.setName(d.name());
        po.setStatus(d.status());
        po.setRemark(d.remark());
        return po;
    }
}
