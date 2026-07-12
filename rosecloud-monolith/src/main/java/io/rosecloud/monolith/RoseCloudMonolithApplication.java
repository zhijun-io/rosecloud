package io.rosecloud.monolith;

import io.rosecloud.api.user.UserTenantFeignApi;
import io.rosecloud.auth.RoseCloudAuthApplication;
import io.rosecloud.notice.RoseCloudNoticeApplication;
import io.rosecloud.system.RoseCloudSystemApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@ComponentScan(basePackages = {"io.rosecloud.monolith", "io.rosecloud.auth", "io.rosecloud.system", "io.rosecloud.notice"},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {RoseCloudAuthApplication.class, RoseCloudSystemApplication.class,
                        RoseCloudNoticeApplication.class}))
// The monolith excludes the three service @SpringBootApplication classes, so their
// @EnableFeignClients are NOT processed. Re-enable only the client that has no in-process
// provider: UserTenantApi is consumed by auth's TenantSelectionService but the system
// UserTenantController does not implement the interface, so it must come from the Feign
// client. NoticePublishApi / AuditLogApi already have local @RestController/@Service beans
// (NoticeServiceImpl / AuditLogServiceImpl) in this single context, so enabling their Feign
// clients would create duplicate beans — hence we register only UserTenantFeignApi explicitly.
@EnableFeignClients(clients = UserTenantFeignApi.class)
@SpringBootApplication
public class RoseCloudMonolithApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoseCloudMonolithApplication.class, args);
    }
}
