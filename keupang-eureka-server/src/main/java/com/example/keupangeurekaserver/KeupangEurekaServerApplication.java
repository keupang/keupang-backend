package com.example.keupangeurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class KeupangEurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeupangEurekaServerApplication.class, args);
    }

}
