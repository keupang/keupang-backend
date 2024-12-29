package com.example.keupanguser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class KeupangUserApplication {

    public static void main(String[] args) {
        System.setProperty("security_username", System.getenv("security_username"));
        System.setProperty("security_password", System.getenv("security_password"));
        System.setProperty("DB_HOST", System.getenv("DB_HOST"));
        System.setProperty("DB_PORT", System.getenv("DB_PORT"));
        System.setProperty("DB_USERNAME", System.getenv("DB_USERNAME"));
        System.setProperty("DB_PASSWORD", System.getenv("DB_PASSWORD"));
        System.setProperty("USER_DB_NAME", System.getenv("USER_DB_NAME"));
        System.setProperty("google_username", System.getenv("google_username"));
        System.setProperty("google_password", System.getenv("google_password"));


        SpringApplication.run(KeupangUserApplication.class, args);
    }

}
