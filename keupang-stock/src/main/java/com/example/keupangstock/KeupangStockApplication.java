package com.example.keupangstock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
public class KeupangStockApplication {

    public static void main(String[] args) {
        System.setProperty("AWS_S3_BUCKET", System.getenv("AWS_S3_BUCKET"));
        System.setProperty("AWS_S3_ACCESS_KEY", System.getenv("AWS_S3_ACCESS_KEY"));
        System.setProperty("AWS_S3_SECRET_KEY", System.getenv("AWS_S3_SECRET_KEY"));
        System.setProperty("AWS_S3_REGION", System.getenv("AWS_S3_REGION"));
        SpringApplication.run(KeupangStockApplication.class, args);
    }

}
