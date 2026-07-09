package io.rosecloud.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "io.rosecloud.api")
public class RoseCloudSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoseCloudSystemApplication.class, args);
    }
}
