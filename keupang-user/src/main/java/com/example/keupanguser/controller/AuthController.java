package com.example.keupanguser.controller;

import com.example.keupanguser.request.LoginRequest;
import com.example.keupanguser.service.UserService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@ModelAttribute LoginRequest loginRequest) {
        log.info("LoginRequest: {}", loginRequest);
        log.info("UserService class: {}", userService.getClass()); // Mock 객체 확인
        String token = userService.userLogin(loginRequest);
//        if (token == null){
//            throw new IllegalArgumentException("Invalid email or password.");
//        }
//        return ResponseEntity.ok(Map.of("token", token));
        log.info("Generated Token: {}", token);
        return ResponseEntity.ok(token);
    }
}
