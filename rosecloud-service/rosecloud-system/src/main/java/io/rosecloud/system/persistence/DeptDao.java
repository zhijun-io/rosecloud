package io.rosecloud.system.persistence;

import io.rosecloud.starter.data.dao.MyBatisDao;
import java.util.List;
import io.rosecloud.system.domain.Dept;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;

@Repository
public class DeptDao extends MyBatisDao<Dept, Long, DeptEntity> {

    // ==== 父部门检查 ====

    public boolean existsByParentId(Long parentId) {
        return mapper.exists(new LambdaQueryWrapper<DeptEntity>().eq(DeptEntity::getParentId, parentId));
    }

    // ==== 全量查询 ====

    public List<Dept> findAllOrderBySort() {
        return mapper.selectList(new LambdaQueryWrapper<DeptEntity>()
                        .orderByAsc(DeptEntity::getSort)
                        .orderByAsc(DeptEntity::getId))
                .stream().map(DeptEntity::toData).toList();
    }

    public DeptDao(DeptMapper deptMapper) {
        super(deptMapper, DeptEntity.class);
    }

    @Override
    protected Long getId(Dept domain) {
        return domain.getId();
    }

    @Override
    protected DeptEntity toEntity(Dept domain) {
        return new DeptEntity().toEntity(domain);
    }
}
