package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.system.domain.TenantProfile;
import io.rosecloud.system.domain.TenantProfileData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantProfileRepositoryImplTest {

    @Mock
    TenantProfileMapper mapper;

    @Test
    void findAllMapsTenantProfiles() {
        TenantProfileEntity po = new TenantProfileEntity();
        po.setId("default");
        po.setName("Basic");
        po.setDescription("Default tier");
        po.setAdditionalInfo("{\"packageCode\":\"basic\",\"maxUsers\":10,\"maxRoles\":5,"
                + "\"maxNoticesPerDay\":100,\"maxRequestsPerMinute\":60,\"enabledCapabilities\":[]}");
        po.setIsDefault(1);

        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(po));

        TenantProfileRepositoryImpl repository = new TenantProfileRepositoryImpl(mapper, new ObjectMapper());

        TenantProfile profile = repository.findAll().getFirst();

        assertEquals("default", profile.getId());
        assertEquals(true, profile.isDefault());
        assertEquals(TenantProfileData.defaults(), profile.getProfileData());
        assertEquals(10, profile.getAdditionalInfo().get("maxUsers").asInt());
    }

    @Test
    void findDefaultMapsDefaultProfile() {
        TenantProfileEntity po = new TenantProfileEntity();
        po.setId("default");
        po.setName("Basic");
        po.setDescription("Default tier");
        po.setAdditionalInfo("{\"packageCode\":\"basic\",\"maxUsers\":10,\"maxRoles\":5,"
                + "\"maxNoticesPerDay\":100,\"maxRequestsPerMinute\":60,\"enabledCapabilities\":[]}");
        po.setIsDefault(1);

        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(po);

        TenantProfileRepositoryImpl repository = new TenantProfileRepositoryImpl(mapper, new ObjectMapper());

        Optional<TenantProfile> profile = repository.findDefault();

        assertEquals("default", profile.orElseThrow().getId());
        assertEquals(true, profile.orElseThrow().isDefault());
    }

    @Test
    void defaultProfileIdThrowsWhenDefaultMissing() {
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        TenantProfileRepositoryImpl repository = new TenantProfileRepositoryImpl(mapper, new ObjectMapper());

        assertThrows(io.rosecloud.common.core.error.BizException.class, repository::defaultProfileId);
    }
}
