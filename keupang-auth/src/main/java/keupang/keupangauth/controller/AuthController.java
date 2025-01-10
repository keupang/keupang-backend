package keupang.keupangauth.controller;

import java.time.Duration;
import java.util.Map;
import keupang.keupangauth.exception.CustomException;
import keupang.keupangauth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String ,String> redisTemplate;

    private static final long JWT_EXPIRE_TIME = 2; // JWT 만료 시간

    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> generateToken(
        @RequestParam("email") String email,
        @RequestParam("role") String role) {

        // JWT 생성
        String token = jwtTokenProvider.createToken(email, role);

        // Redis 에 저장
        String redisKey = "auth:jwt:" + email;
        redisTemplate.opsForValue().set(redisKey, token, Duration.ofHours(JWT_EXPIRE_TIME));

        log.info("JWT 생성 완료: {}", token);

        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, 40101,
                "Token is missing or invalid.", "Provide a valid token.", "INVALID_TOKEN");
        }

        // Bearer 제거
        token = token.substring(7);

        // JWT 검증
        if (!jwtTokenProvider.validateToken(token)) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, 40102,
                "Token validation failed.", "Provide a valid token.", "INVALID_TOKEN");
        }

        // Redis에서 토큰 확인
        String email = jwtTokenProvider.getEmail(token);
        String redisKey = "auth:jwt:" + email;
        String storedToken = redisTemplate.opsForValue().get(redisKey);

        if (storedToken == null || !storedToken.equals(token)) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, 40103,
                "Token is expired or invalidated.", "Log in again.", "TOKEN_EXPIRED");
        }

        // 응답 데이터
        String role = jwtTokenProvider.getRole(token);
        return ResponseEntity.ok(Map.of("email", email, "role", role));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new CustomException(HttpStatus.BAD_REQUEST, 40001,
                "Token is missing or invalid.", "Provide a valid token.", "INVALID_TOKEN");
        }

        // Bearer 제거
        token = token.substring(7);

        // Redis에서 토큰 삭제
        String email = jwtTokenProvider.getEmail(token);
        String redisKey = "auth:jwt:" + email;
        redisTemplate.delete(redisKey);

        log.info("JWT 로그아웃 완료: {}", token);

        return ResponseEntity.ok(email);
    }
}
