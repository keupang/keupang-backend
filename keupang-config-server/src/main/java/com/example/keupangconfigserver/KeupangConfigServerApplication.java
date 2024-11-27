package com.example.keupangconfigserver;

import java.util.Base64;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class KeupangConfigServerApplication {

    public static void main(String[] args) {
        String base64Key = System.getenv("private_key");
        String privateKey = new String(Base64.getDecoder().decode(base64Key));

        System.setProperty("private_key", privateKey);
        SpringApplication.run(KeupangConfigServerApplication.class, args);
    }

}
