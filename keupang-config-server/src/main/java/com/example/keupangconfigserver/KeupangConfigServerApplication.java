package com.example.keupangconfigserver;

import java.util.Base64;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@EnableConfigServer
public class KeupangConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeupangConfigServerApplication.class, args);
    }

}
