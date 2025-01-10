package com.example.keupanguser.service;

import com.example.keupanguser.client.AuthClient;
import com.example.keupanguser.domain.Role;
import com.example.keupanguser.domain.User;
import com.example.keupanguser.exception.CustomException;
import com.example.keupanguser.repository.UserRepository;
import com.example.keupanguser.request.LoginRequest;
import com.example.keupanguser.request.UserRequest;
import com.example.keupanguser.response.LoginResponse;
import feign.FeignException;
import java.time.Duration;
import java.util.Map;
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
    private final RedisTemplate<String, Object> redisTemplate;
    private final AuthClient authClient;
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

        User newUser = User.builder()
            .userName(user.getUserName())
            .userEmail(user.getUserEmail())
            .role(Role.USER)
            .userPhone(user.getUserPhone())
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
        // Auth Service 로 JWT 요청
        try {
            Map<String, String> tokenResponse = authClient.generateToken(
                user.getUserEmail(),
                user.getRole().name()
            );

            String token = tokenResponse.get("token");
            log.info("JWT 생성 완료: {}", token);

            return new LoginResponse(user.getUserName(), token);
        } catch (FeignException ex) {
            throw new CustomException(HttpStatus.SERVICE_UNAVAILABLE, 50301,
                "현재 인증 서비스를 이용할 수 없습니다.", "담당자에게 문의 후 서비스 다시 시도해주시기 바랍니다.", "SERVICE_UNAVAILABLE");
        }
    }

    public String logout(String token) {
        // Auth Service 에 로그아웃 요청
        try {
            String email = authClient.logout("Bearer " + token);
            log.info("JWT 로그아웃 완료: {}", token);
            return email;
        } catch (Exception e) {
            throw new CustomException(HttpStatus.SERVICE_UNAVAILABLE, 50301,
                "현재 인증 서비스를 이용할 수 없습니다.", "담당자에게 문의 후 서비스 다시 시도해주시기 바랍니다.", "SERVICE_UNAVAILABLE");
        }
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
