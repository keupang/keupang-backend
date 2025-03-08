package com.example.keupangstock.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service", url = "https://api.keupang.store/api/auth")
public interface AuthClient {
    @PostMapping("/validate")
    Map<String, Object> validateToken(@RequestHeader("Authorization") String token);
}
