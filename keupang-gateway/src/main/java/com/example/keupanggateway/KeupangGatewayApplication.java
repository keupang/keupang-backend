package com.example.keupanggateway;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class KeupangGatewayApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure().directory("./")  // .env 파일 경로 설정
			.load();

		System.setProperty("security_username", dotenv.get("security_username"));
		System.setProperty("security_password", dotenv.get("security_password"));
		SpringApplication.run(KeupangGatewayApplication.class, args);
	}

}
