package com.example.keupanguser.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service", url = "https://api.keupang.store/api/auth")
public interface AuthClient {
    @PostMapping("/token")
    Map<String, String> generateToken(@RequestParam("email") String email, @RequestParam("role") String role);

    @PostMapping("/validate")
    Map<String, Object> validateToken(@RequestHeader("Authorization") String token);

    @PostMapping("/logout")
    String logout(@RequestHeader("Authorization") String token);
}
