package io.rosecloud.system.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantRepositoryImplTest {

    @Mock
    TenantMapper mapper;

    @Test
    void findByIdMarksEnabledTenantAsExpiredAfterExpireDate() {
        TenantEntity po = new TenantEntity();
        po.setId("tenant-7");
        po.setName("Acme");
        po.setStatus(TenantStatus.ENABLED.code());
        po.setExpireTime(LocalDate.now().minusDays(1));
        po.setExtra("{\"tier\":\"gold\"}");

        when(mapper.selectById("tenant-7")).thenReturn(po);

        TenantRepositoryImpl repository = new TenantRepositoryImpl(mapper, new ObjectMapper());

        Tenant tenant = repository.findById("tenant-7").orElseThrow();

        assertEquals(TenantStatus.EXPIRED, tenant.getStatus());
        assertEquals(LocalDate.now().minusDays(1), tenant.getExpireTime());
        assertEquals("gold", tenant.getAdditionalInfo().get("tier").asText());
    }

    @Test
    void pageMapsTenantRecords() {
        TenantEntity po = new TenantEntity();
        po.setId("tenant-8");
        po.setName("Beta");
        po.setStatus(TenantStatus.PENDING.code());
        po.setExpireTime(LocalDate.now().plusDays(10));

        when(mapper.selectPage(any(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Page<TenantEntity> page = invocation.getArgument(0);
            page.setRecords(List.of(po));
            page.setTotal(1L);
            return page;
        });

        TenantRepositoryImpl repository = new TenantRepositoryImpl(mapper, new ObjectMapper());

        assertEquals(TenantStatus.PENDING, repository.page(1, 10, null).records().get(0).getStatus());
    }
}
