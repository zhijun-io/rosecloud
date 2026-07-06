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
        return mapper.exists(new LambdaQueryWrapper<DeptPO>().eq(DeptPO::getParentId, parentId));
    }

    @Override
    public Long insert(Dept dept) {
        DeptPO po = toPO(dept);
        po.setId(null);
        mapper.insert(po);
        return po.getId();
    }

    @Override
    public void update(Dept dept) {
        mapper.updateById(toPO(dept));
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public List<Dept> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<DeptPO>()
                .orderByAsc(DeptPO::getSort)
                .orderByAsc(DeptPO::getId)).stream().map(this::toDomain).toList();
    }

    private Dept toDomain(DeptPO po) {
        return new Dept(po.getId(), po.getParentId(), po.getName(), po.getSort(),
                po.getStatus(), po.getLeader(), po.getPhone());
    }

    private DeptPO toPO(Dept d) {
        DeptPO po = new DeptPO();
        po.setId(d.id());
        po.setParentId(d.parentId());
        po.setName(d.name());
        po.setSort(d.sort());
        po.setStatus(d.status());
        po.setLeader(d.leader());
        po.setPhone(d.phone());
        return po;
    }
}
