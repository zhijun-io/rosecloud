package io.rosecloud.notice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients(basePackages = "io.rosecloud.api")
@SpringBootApplication
@MapperScan("io.rosecloud.notice.persistence")
@EnableScheduling
public class RoseCloudNoticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoseCloudNoticeApplication.class, args);
    }
}
