package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.system.domain.Dept;
import io.rosecloud.system.domain.DeptRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DeptRepositoryImpl implements DeptRepository {

    private final DeptMapper mapper;

    public DeptRepositoryImpl(DeptMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Dept> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public boolean existsByParentId(Long parentId) {
        return mapper.exists(new LambdaQueryWrapper<DeptEntity>().eq(DeptEntity::getParentId, parentId));
    }

    @Override
    public Long insert(Dept dept) {
        DeptEntity po = toEntity(dept);
        po.setId(null);
        mapper.insert(po);
        return po.getId();
    }

    @Override
    public void update(Dept dept) {
        mapper.updateById(toEntity(dept));
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public List<Dept> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<DeptEntity>()
                .orderByAsc(DeptEntity::getSort)
                .orderByAsc(DeptEntity::getId)).stream().map(this::toDomain).toList();
    }

    private Dept toDomain(DeptEntity po) {
        return new Dept(po.getId(), po.getParentId(), po.getName(), po.getSort(),
                po.getStatus(), po.getLeader(), po.getPhone(), po.getCreateTime(), po.getCreateBy(),
                po.getUpdateTime(), po.getUpdateBy());
    }

    private DeptEntity toEntity(Dept d) {
        DeptEntity po = new DeptEntity();
        po.setId(d.getId());
        po.setParentId(d.getParentId());
        po.setName(d.getName());
        po.setSort(d.getSort());
        po.setStatus(d.getStatus());
        po.setLeader(d.getLeader());
        po.setPhone(d.getPhone());
        po.setCreateTime(d.getCreateTime());
        po.setCreateBy(d.getCreateBy());
        po.setUpdateTime(d.getUpdateTime());
        po.setUpdateBy(d.getUpdateBy());
        return po;
    }
}
