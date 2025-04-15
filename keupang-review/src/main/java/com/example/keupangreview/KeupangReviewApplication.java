package com.example.keupangreview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;


@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class KeupangReviewApplication {

	public static void main(String[] args) {
		System.setProperty("security_username", System.getenv("security_username"));
		System.setProperty("security_password", System.getenv("security_password"));
		System.setProperty("DB_HOST", System.getenv("DB_HOST"));
		System.setProperty("DB_PORT", System.getenv("DB_PORT"));
		System.setProperty("DB_USERNAME", System.getenv("DB_USERNAME"));
		System.setProperty("DB_PASSWORD", System.getenv("DB_PASSWORD"));
		System.setProperty("REVIEW_DB_NAME", System.getenv("REVIEW_DB_NAME"));
		SpringApplication.run(KeupangReviewApplication.class, args);
	}

}
