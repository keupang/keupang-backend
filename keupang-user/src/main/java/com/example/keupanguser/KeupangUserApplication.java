package com.example.keupanguser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class KeupangUserApplication {

    public static void main(String[] args) {
        setSystemProperty("security_username");
        setSystemProperty("security_password");
        setSystemProperty("DB_HOST");
        setSystemProperty("DB_PORT");
        setSystemProperty("DB_USERNAME");
        setSystemProperty("DB_PASSWORD");
        setSystemProperty("USER_DB_NAME");
        setSystemProperty("google_username");
        setSystemProperty("google_password");

        SpringApplication.run(KeupangUserApplication.class, args);
    }

    private static void setSystemProperty(String key) {
        String value = System.getenv(key);
        if (value != null) {
            System.setProperty(key, value);
        }
    }

}
