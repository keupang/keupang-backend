package com.example.keupangconfigserver;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.Base64;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class KeupangConfigServerApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
            .directory("./")  // .env 파일 경로 설정
            .load();
        // Base64 복호화
        String base64Key = dotenv.get("private_key");
        String privateKey = new String(Base64.getDecoder().decode(base64Key));

        System.setProperty("private_key", privateKey);
        SpringApplication.run(KeupangConfigServerApplication.class, args);
    }

}
