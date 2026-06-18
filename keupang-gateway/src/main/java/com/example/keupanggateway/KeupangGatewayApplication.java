package com.example.keupanggateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class KeupangGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(KeupangGatewayApplication.class, args);
	}

}
