package io.rosecloud.monolith;

import io.rosecloud.auth.RoseCloudAuthApplication;
import io.rosecloud.notice.RoseCloudNoticeApplication;
import io.rosecloud.system.RoseCloudSystemApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@ComponentScan(basePackages = {"io.rosecloud.monolith", "io.rosecloud.auth", "io.rosecloud.system", "io.rosecloud.notice"},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {RoseCloudAuthApplication.class, RoseCloudSystemApplication.class,
                        RoseCloudNoticeApplication.class}))
@SpringBootApplication
public class RoseCloudMonolithApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoseCloudMonolithApplication.class, args);
    }
}
