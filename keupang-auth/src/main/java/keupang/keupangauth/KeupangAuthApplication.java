package keupang.keupangauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class KeupangAuthApplication {

	public static void main(String[] args) {
		System.setProperty("security_username", System.getenv("security_username"));
		System.setProperty("security_password", System.getenv("security_password"));

		SpringApplication.run(KeupangAuthApplication.class, args);
	}

}
