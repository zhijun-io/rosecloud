package io.rosecloud.system;

import io.rosecloud.api.credential.CredentialApi;
import io.rosecloud.api.log.LoginLogFeignApi;
import io.rosecloud.starter.security.session.LoginSessionFeignApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(clients = {CredentialApi.class, LoginLogFeignApi.class, LoginSessionFeignApi.class})
public class RoseCloudSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoseCloudSystemApplication.class, args);
    }
}
