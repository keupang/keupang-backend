package com.example.keupanguser.controller;

import com.example.keupanguser.request.LoginRequest;
import com.example.keupanguser.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
        log.info("Generated Token: {}", token);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        log.info("로그아웃 요청: {}", token);

        // Bearer 토큰에서 실제 JWT 추출
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        userService.logout(token);
        return ResponseEntity.ok("로그아웃 성공");
    }
}
