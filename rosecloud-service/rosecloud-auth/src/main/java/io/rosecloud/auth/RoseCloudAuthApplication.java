package io.rosecloud.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Auth service entry point. Persistence-free: user credentials live in the
 * system service and are fetched over Feign (see LoginUserLookupImpl). Feign
 * client scanning is scoped to {@code io.rosecloud.api} where contracts live.
 */
@SpringBootApplication
public class RoseCloudAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoseCloudAuthApplication.class, args);
    }
}
