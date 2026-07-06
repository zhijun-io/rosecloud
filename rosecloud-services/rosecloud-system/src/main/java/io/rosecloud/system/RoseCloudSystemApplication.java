package io.rosecloud.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
@MapperScan("io.rosecloud.system.persistence")
public class RoseCloudSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoseCloudSystemApplication.class, args);
    }
}
