package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.Config;
import io.rosecloud.system.domain.ConfigRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ConfigRepositoryImpl implements ConfigRepository {

    private final ConfigMapper mapper;

    public ConfigRepositoryImpl(ConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean existsByKey(String key) {
        return mapper.exists(new LambdaQueryWrapper<ConfigPO>().eq(ConfigPO::getConfigKey, key));
    }

    @Override
    public Long insert(Config config) {
        ConfigPO po = toPO(config);
        po.setId(null);
        mapper.insert(po);
        return po.getId();
    }

    @Override
    public void update(Config config) {
        mapper.updateById(toPO(config));
    }

    @Override
    public Optional<Config> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<Config> findByKey(String key) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<ConfigPO>().eq(ConfigPO::getConfigKey, key))).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public PageResult<Config> page(long current, long size, String keyword) {
        Page<ConfigPO> page = new Page<>(current, size);
        LambdaQueryWrapper<ConfigPO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(ConfigPO::getConfigKey, keyword).or().like(ConfigPO::getDescription, keyword);
        }
        wrapper.orderByDesc(ConfigPO::getCreateTime);
        IPage<ConfigPO> result = mapper.selectPage(page, wrapper);
        List<Config> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private Config toDomain(ConfigPO po) {
        return new Config(po.getId(), po.getConfigKey(), po.getConfigValue(), po.getDescription());
    }

    private ConfigPO toPO(Config c) {
        ConfigPO po = new ConfigPO();
        po.setId(c.id());
        po.setConfigKey(c.configKey());
        po.setConfigValue(c.configValue());
        po.setDescription(c.description());
        return po;
    }
}
