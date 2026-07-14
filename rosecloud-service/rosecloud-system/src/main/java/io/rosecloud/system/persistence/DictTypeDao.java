package io.rosecloud.system.persistence;

import io.rosecloud.starter.data.dao.MyBatisDao;
import io.rosecloud.system.domain.DictType;
import org.springframework.stereotype.Repository;

@Repository
public class DictTypeDao extends MyBatisDao<DictType, Long, DictTypeEntity> {

    public DictTypeDao(DictTypeMapper dictTypeMapper) {
        super(dictTypeMapper, DictTypeEntity.class);
    }

    @Override
    protected Long getId(DictType domain) {
        return domain.getId();
    }

    @Override
    protected DictTypeEntity toEntity(DictType domain) {
        return new DictTypeEntity().toEntity(domain);
    }
}
