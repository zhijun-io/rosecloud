package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        TenantPO po = new TenantPO();
        po.setId(7L);
        po.setName("Acme");
        po.setCode("acme");
        po.setStatus(TenantStatus.ENABLED.code());
        po.setExpireTime(LocalDate.now().minusDays(1));

        when(mapper.selectById(7L)).thenReturn(po);

        TenantRepositoryImpl repository = new TenantRepositoryImpl(mapper);

        Tenant tenant = repository.findById(7L).orElseThrow();

        assertEquals(TenantStatus.EXPIRED, tenant.status());
        assertEquals(LocalDate.now().minusDays(1), tenant.expireTime());
    }

    @Test
    void pageMapsTenantRecords() {
        TenantPO po = new TenantPO();
        po.setId(8L);
        po.setName("Beta");
        po.setCode("beta");
        po.setStatus(TenantStatus.PENDING.code());
        po.setExpireTime(LocalDate.now().plusDays(10));

        when(mapper.selectPage(any(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Page<TenantPO> page = invocation.getArgument(0);
            page.setRecords(List.of(po));
            page.setTotal(1L);
            return page;
        });

        TenantRepositoryImpl repository = new TenantRepositoryImpl(mapper);

        assertEquals(TenantStatus.PENDING, repository.page(1, 10, null).records().get(0).status());
    }
}
