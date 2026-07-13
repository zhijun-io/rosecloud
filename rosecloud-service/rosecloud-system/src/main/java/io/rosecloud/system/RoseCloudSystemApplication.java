package io.rosecloud.system;

import io.rosecloud.api.credential.CredentialApi;
import io.rosecloud.api.log.LoginLogFeignApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(clients = {CredentialApi.class, LoginLogFeignApi.class})
public class RoseCloudSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoseCloudSystemApplication.class, args);
    }
}
