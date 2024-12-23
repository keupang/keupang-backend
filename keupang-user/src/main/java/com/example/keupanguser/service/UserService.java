package com.example.keupanguser.service;

import com.example.keupanguser.domain.User;
import com.example.keupanguser.jwt.JwtTokenProvider;
import com.example.keupanguser.repository.UserRepository;
import com.example.keupanguser.request.LoginRequest;
import com.example.keupanguser.request.UserRequest;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, String> verificationCodes = new HashMap<>();

    public User registerUser(UserRequest user) {
        log.debug("userPassword: {}", user.getUserPassword());
        if (userRepository.existsByUserEmail(user.getUserEmail())) {
            throw new IllegalStateException("Email is already taken.");
        }

        User newUser = User.builder().userName(user.getUserName()).userEmail(user.getUserEmail())
            .role(user.getRole()).userPhone(user.getUserPhone())
            .userPassword(passwordEncoder.encode(user.getUserPassword())).build();
        return userRepository.save(newUser);
    }

    public String userLogin(LoginRequest loginRequest) {
        // 이메일로 사용자 조회
        User user = userRepository.findByUserEmail(loginRequest.userEmail())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        // 비밀번호 검증
        if (!passwordEncoder.matches(loginRequest.userPassword(), user.getUserPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtTokenProvider.createToken(loginRequest.userEmail(),
            String.valueOf(user.getRole()));

        // Redis에 토큰 저장
        String redisKey = "user:token:" + user.getUserEmail();
        redisTemplate.opsForValue().set(redisKey, token, Duration.ofHours(2)); // 2시간 만료

        log.info("redis 저장 : {} = {}", redisKey, token);
        return token;
    }

    public void logout(String userEmail) {
        String redisKey = "user:token:" + userEmail;
        redisTemplate.delete(redisKey);
        log.info("Redis에서 삭제: {}", redisKey);
    }

    public String generateVerificationCode(String email){
        String code = String.format("%06d", (int)(Math.random() * 1_000_000));
        verificationCodes.put(email, code);
        log.info("Generated verification code for {}: {}", email, code);
        return code;
    }

    public boolean verifyCode(String email, String code){
        String savedCode = verificationCodes.get(email);
        if(savedCode != null && savedCode.equals(code)){
            verificationCodes.remove(email);
            log.info("Email {} verified successfully.", email);
            return true;
        }
        log.warn("Verification failed for email {}: provided code {}", email, code);
        return false;
    }
}
