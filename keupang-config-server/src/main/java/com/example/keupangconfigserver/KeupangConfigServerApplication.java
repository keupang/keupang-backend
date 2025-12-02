package com.example.keupangconfigserver;

import java.util.Base64;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class KeupangConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeupangConfigServerApplication.class, args);
    }

}
