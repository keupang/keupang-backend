package com.example.keupanggateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class KeupangGatewayApplication {

	public static void main(String[] args) {
		System.setProperty("security_username", System.getenv("security_username"));
		System.setProperty("security_password", System.getenv("security_password"));
		SpringApplication.run(KeupangGatewayApplication.class, args);
	}

}
