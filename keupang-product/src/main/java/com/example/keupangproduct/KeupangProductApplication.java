package com.example.keupangproduct;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class KeupangProductApplication {

    public static void main(String[] args) {
        setSystemProperty("security_username");
        setSystemProperty("security_password");
        setSystemProperty("DB_HOST");
        setSystemProperty("DB_PORT");
        setSystemProperty("DB_USERNAME");
        setSystemProperty("DB_PASSWORD");
        setSystemProperty("PRODUCT_DB_NAME");
        SpringApplication.run(KeupangProductApplication.class, args);
    }

    private static void setSystemProperty(String key) {
        String value = System.getenv(key);
        if (value != null) {
            System.setProperty(key, value);
        }
    }
}
