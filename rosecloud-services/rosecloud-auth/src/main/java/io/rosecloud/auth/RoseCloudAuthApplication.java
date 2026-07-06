package io.rosecloud.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class RoseCloudAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoseCloudAuthApplication.class, args);
    }
}

