package io.rosecloud.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
@MapperScan("io.rosecloud.auth.persistence")
public class RoseCloudAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoseCloudAuthApplication.class, args);
    }
}
