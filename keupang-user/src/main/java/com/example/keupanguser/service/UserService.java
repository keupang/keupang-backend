package com.example.keupanguser.service;

import com.example.keupanguser.domain.User;
import com.example.keupanguser.exception.CustomException;
import com.example.keupanguser.jwt.JwtTokenProvider;
import com.example.keupanguser.repository.UserRepository;
import com.example.keupanguser.request.LoginRequest;
import com.example.keupanguser.request.UserRequest;
import com.example.keupanguser.response.LoginResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
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
    private static final long JWT_EXPIRE_TIME = 2; // JWT 만료 시간
    private static final long EMAIL_CODE_EXPIRE_TIME = 5; // EMAIL_CODE 만료 시간


    public User registerUser(UserRequest user) {
        log.debug("userPassword: {}", user.getUserPassword());
        if (userRepository.existsByUserEmail(user.getUserEmail())) {
            throw new CustomException(
                HttpStatus.UNAUTHORIZED,
                40104,
                "중복된 이메일을 등록하였습니다.", //detail
                "다른 이메일로 회원가입을 시도해주세요", //help
                "DUPLICATED_USER_EMAIL" //message
            );
        }

        User newUser = User.builder().userName(user.getUserName()).userEmail(user.getUserEmail())
            .role(user.getRole()).userPhone(user.getUserPhone())
            .userPassword(passwordEncoder.encode(user.getUserPassword())).build();
        return userRepository.save(newUser);
    }

    public LoginResponse userLogin(LoginRequest loginRequest) {
        // 이메일로 사용자 조회
        User user = userRepository.findByUserEmail(loginRequest.userEmail())
            .orElseThrow(() -> new CustomException(
                HttpStatus.UNAUTHORIZED,
                40102,
                "이메일 또는 비밀번호를 잘못 입력하였습니다.",
                "이메일이나 비밀번호를 다시 입력해주세요.",
                "INVALID_EMAIL_OR_PASSWORD"
            ));

        // 비밀번호 검증
        if (!passwordEncoder.matches(loginRequest.userPassword(), user.getUserPassword())) {
            throw new CustomException(
                HttpStatus.UNAUTHORIZED,
                40102,
                "이메일 또는 비밀번호를 잘못 입력하였습니다.",
                "이메일이나 비밀번호를 다시 입력해주세요.",
                "INVALID_EMAIL_OR_PASSWORD"
            );
        }

        String token = jwtTokenProvider.createToken(loginRequest.userEmail(),
            String.valueOf(user.getRole()));

        // Redis에 토큰 저장
        String redisKey = "user:token:" + user.getUserEmail();
        redisTemplate.opsForValue()
            .set(redisKey, token, Duration.ofHours(JWT_EXPIRE_TIME)); //JWT 만료

        log.info("redis 저장 : {} = {}", redisKey, token);

        return new LoginResponse(user.getUserName(), token);
    }

    public String logout(String token) {
        String redisKey = "user:token:" + token;
        redisTemplate.delete(redisKey);
        log.info("Redis에서 삭제: {}", redisKey);

        return jwtTokenProvider.getEmail(token);
    }

    public String generateVerificationCode(String email) {
        String code = String.format("%06d", (int) (Math.random() * 1_000_000));
        String redisKey = "email:verification:" + email;
        redisTemplate.opsForValue()
            .set(redisKey, code, Duration.ofMinutes(EMAIL_CODE_EXPIRE_TIME)); //이메일 인증 코드 만료
        log.info("Generated verification code to Redis {}: {}", redisKey, code);
        return code;
    }

    public boolean verifyCode(String email, String code) {
        String redisKey = "email:verification:" + email;
        String savedCode = (String) redisTemplate.opsForValue().get(redisKey);
        if (savedCode != null && savedCode.equals(code)) {
            redisTemplate.delete(redisKey); //인증 후 코드 삭제
            log.info("Verification code for {} verified successfully.", email);
            return true;
        }
        log.warn("Verification failed for email {}: provided code {}", email, code);
        return false;
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }
}
