package io.rosecloud.notice;

import io.rosecloud.api.audit.AuditLogFeignApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(clients = AuditLogFeignApi.class)
public class RoseCloudNoticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoseCloudNoticeApplication.class, args);
    }
}
