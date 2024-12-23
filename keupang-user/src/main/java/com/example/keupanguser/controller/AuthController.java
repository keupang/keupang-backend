package com.example.keupanguser.controller;

import com.example.keupanguser.request.LoginRequest;
import com.example.keupanguser.service.EmailService;
import com.example.keupanguser.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        log.info("LoginRequest: {}", loginRequest);
        log.info("UserService class: {}", userService.getClass()); // Mock 객체 확인
        String token = userService.userLogin(loginRequest);
        log.info("Generated Token: {}", token);

        // JSON 형태로 반환
        Map<String, String> response = Map.of("token", token);
        return ResponseEntity.ok(response);
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

    @PostMapping("/send-verification-email")
    public ResponseEntity<?> sendVerificationEmail(@RequestParam String email) {
        // 인증 토큰
        String verificationCode = userService.generateVerificationCode(email);

        // 이메일 내용
        String subject = "Keupang 이메일 인증";
        String body = "<h1>Your Verification Code</h1>"
            + "<p>Your verification code is:</p>"
            + "<h2>" + verificationCode + "</h2>";

        // 이메일 전송
        emailService.sendEmail(email, subject, body);
        return ResponseEntity.ok("인증 번호가" + email + "로 전송되었습니다.");
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String email, @RequestParam String code) {
        // 인증 코드 검증
        boolean isVerified = userService.verifyCode(email, code);

        if (isVerified) {
            return ResponseEntity.ok("이메일 인증이 완료되었습니다.");
        } else {
            return ResponseEntity.badRequest().body("유효하지 않은 인증 번호 입니다.");
        }
    }
}
