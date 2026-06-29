package com.example.keupangreview.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user", path = "/api/user")
public interface UserClient {
    @GetMapping("/name")
    String getName(@RequestParam("email") String userEmail);
}
