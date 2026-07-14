package io.rosecloud.system.persistence;

import io.rosecloud.starter.data.dao.MyBatisDao;
import java.util.List;
import io.rosecloud.system.domain.DictData;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;

@Repository
public class DictDataDao extends MyBatisDao<DictData, Long, DictDataEntity> {

    // ==== 字典项查询 ====

    public List<DictData> findByCode(String dictCode) {
        return mapper.selectList(new LambdaQueryWrapper<DictDataEntity>()
                        .eq(DictDataEntity::getDictCode, dictCode)
                        .eq(DictDataEntity::getStatus, 1)
                        .orderByAsc(DictDataEntity::getSort))
                .stream().map(DictDataEntity::toData).toList();
    }

    public void deleteByCode(String dictCode) {
        mapper.delete(new LambdaQueryWrapper<DictDataEntity>()
                .eq(DictDataEntity::getDictCode, dictCode));
    }

    public DictDataDao(DictDataMapper dictDataMapper) {
        super(dictDataMapper, DictDataEntity.class);
    }

    @Override
    protected Long getId(DictData domain) {
        return domain.getId();
    }

    @Override
    protected DictDataEntity toEntity(DictData domain) {
        return new DictDataEntity().toEntity(domain);
    }
}
