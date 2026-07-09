package io.rosecloud.system.persistence;

import io.rosecloud.system.domain.SettingKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingKeyRepositoryImplTest {

    @Mock
    SettingKeyMapper mapper;

    @Test
    void updateUsesPersistentIdLookedUpByKey() {
        SettingKeyEntity current = new SettingKeyEntity();
        current.setId(101L);
        current.setKey("ui.theme");
        current.setName("主题");
        current.setRemark("old");
        when(mapper.selectOne(any())).thenReturn(current);

        new SettingKeyRepositoryImpl(mapper)
                .update(new SettingKey(null, "ui.theme", "主题2", "new"));

        ArgumentCaptor<SettingKeyEntity> captor = ArgumentCaptor.forClass(SettingKeyEntity.class);
        verify(mapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(101L);
        assertThat(captor.getValue().getKey()).isEqualTo("ui.theme");
        assertThat(captor.getValue().getName()).isEqualTo("主题2");
        assertThat(captor.getValue().getRemark()).isEqualTo("new");
    }

    @Test
    void deleteResolvesPersistentIdFromKey() {
        SettingKeyEntity current = new SettingKeyEntity();
        current.setId(202L);
        current.setKey("ui.theme");
        when(mapper.selectOne(any())).thenReturn(current);

        new SettingKeyRepositoryImpl(mapper).deleteByKey("ui.theme");

        verify(mapper).deleteById(202L);
    }
}
