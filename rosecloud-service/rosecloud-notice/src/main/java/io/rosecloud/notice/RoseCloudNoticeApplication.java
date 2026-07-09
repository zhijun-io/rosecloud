package io.rosecloud.notice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "io.rosecloud.api")
public class RoseCloudNoticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoseCloudNoticeApplication.class, args);
    }
}
