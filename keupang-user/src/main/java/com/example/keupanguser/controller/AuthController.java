package com.example.keupanguser.controller;

import com.example.keupanguser.exception.CustomException;
import com.example.keupanguser.request.LoginRequest;
import com.example.keupanguser.response.LoginResponse;
import com.example.keupanguser.service.EmailService;
import com.example.keupanguser.service.UserService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        LoginResponse loginResponse = userService.userLogin(loginRequest);
        log.info("Generated Token: {}", loginResponse.token());

        // "content" 필드 값 추가
        Map<String, Object> content = new HashMap<>();
        content.put("detail", "로그인에 성공했습니다.");

        // "data" 필드 값 추가
        Map<String, Object> data = new HashMap<>();
        data.put("token", loginResponse.token());
        data.put("name", loginResponse.userName());

        // 응답 생성
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", 200);
        responseBody.put("code", 20001);
        responseBody.put("message", "SUCCESS_LOGIN");
        responseBody.put("content", content);
        responseBody.put("data", data); // "data" 필드 추가
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        log.info("로그아웃 요청: {}", token);

        // Bearer 토큰에서 실제 JWT 추출
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        String userEmail = userService.logout(token);

        // "content" 필드 값 추가
        Map<String, Object> content = new HashMap<>();
        content.put("detail", "로그아웃에 성공했습니다.");

        // "data" 필드 값 추가
        Map<String, Object> data = new HashMap<>();
        data.put("userEmail", userEmail);

        // 응답 생성
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", 200);
        responseBody.put("code", 20002);
        responseBody.put("message", "SUCCESS_LOGOUT");
        responseBody.put("content", content);
        responseBody.put("data", data); // "data" 필드 추가

        return ResponseEntity.ok(responseBody);
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

        // "content" 필드 값 추가
        Map<String, Object> content = new HashMap<>();
        content.put("detail", "인증 번호가" + email + "로 전송되었습니다.");

        // "data" 필드 값 추가
        Map<String, Object> data = new HashMap<>();
        data.put("userEmail", email);

        // 응답 생성
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", 200);
        responseBody.put("code", 20003);
        responseBody.put("message", "SUCCESS_EMAIL_VERIFICATION_TOKEN_ISSUED");
        responseBody.put("content", content);
        responseBody.put("data", data); // "data" 필드 추가

        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String email, @RequestParam String code) {
        // 인증 코드 검증
        boolean isVerified = userService.verifyCode(email, code);

        if (isVerified) {
            // "content" 필드 값 추가
            Map<String, Object> content = new HashMap<>();
            content.put("detail", "이메일 인증이 완료되었습니다.");

            // "data" 필드 값 추가
            Map<String, Object> data = new HashMap<>();
            data.put("userEmail", email);

            // 응답 생성
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("status", 200);
            responseBody.put("code", 20000);
            responseBody.put("message","SUCCESS_EMAIL_VERIFICATION");
            responseBody.put("content", content);
            responseBody.put("data", data); // "data" 필드 추가

            return ResponseEntity.ok(responseBody);
        } else {
            throw new CustomException(
                HttpStatus.BAD_REQUEST,
                40101,
                "인증 토큰이 올바르지 않거나 만료되었습니다.",
                "인증 번호를 다시 받은 후에 시도 해주세요.",
                "INVALID_VERIFY_TOKEN"
            );
        }
    }
}
