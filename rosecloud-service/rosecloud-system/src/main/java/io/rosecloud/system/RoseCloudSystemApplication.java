package io.rosecloud.system;

import io.rosecloud.api.notice.NoticePublishFeignApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(clients = NoticePublishFeignApi.class)
public class RoseCloudSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoseCloudSystemApplication.class, args);
    }
}
