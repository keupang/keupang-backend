package com.example.keupanguser.service;

import com.example.keupanguser.domain.User;
import com.example.keupanguser.jwt.JwtTokenProvider;
import com.example.keupanguser.repository.UserRepository;
import com.example.keupanguser.request.LoginRequest;
import com.example.keupanguser.request.UserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    public User registerUser(UserRequest user) {
        log.debug("userPassword: {}", user.getUserPassword());
        if (userRepository.existsByUserEmail(user.getUserEmail())) {
            throw new IllegalStateException("Email is already taken.");
        }

        User newUser = User.builder()
            .userName(user.getUserName())
            .userEmail(user.getUserEmail())
            .role(user.getRole())
            .userPhone(user.getUserPhone())
            .userPassword(passwordEncoder.encode(user.getUserPassword()))
            .build();
        return userRepository.save(newUser);
    }

    public String userLogin(LoginRequest loginRequest){
        // 이메일로 사용자 조회
        User user = userRepository.findByUserEmail(loginRequest.userEmail())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        // 비밀번호 검증
        if (!passwordEncoder.matches(loginRequest.userPassword(), user.getUserPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        return jwtTokenProvider.createToken(loginRequest.userEmail(),
            String.valueOf(user.getRole()));

    }

}
