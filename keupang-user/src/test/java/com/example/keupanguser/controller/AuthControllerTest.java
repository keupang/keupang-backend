package com.example.keupanguser.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.keupanguser.domain.Role;
import com.example.keupanguser.domain.User;
import com.example.keupanguser.repository.UserRepository;
import com.example.keupanguser.request.LoginRequest;
import com.example.keupanguser.service.UserService;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest(properties = {
    "spring.config.name=application-test"
})
class AuthControllerTest {
    @Mock
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();

        // Given: H2 DB에 테스트 데이터를 삽입
        User user = User.builder()
            .userEmail("cj855695@gmail.com")
            .userPassword(passwordEncoder.encode("1234")) // 실제 환경에서는 암호화를 적용해야 함
            .userName("Test User")
            .userPhone("010-1234-5678")
            .role(Role.USER)
            .build();
        User save = userRepository.save(user);// UserRepository는 JPA 레포지토리로 가정
        System.out.println("save = " + save);
    }
    @Test
    void 로그인_성공() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("cj855695@gmail.com", "1234");
        String mockToken = "mockedJwtToken";
        when(userService.userLogin(loginRequest)).thenReturn(mockToken);
        System.out.println("Mock Token: " + userService.userLogin(new LoginRequest("cj855695@gmail.com", "1234")));
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .param("userEmail", "cj855695@gmail.com")
                .param("userPassword", "1234")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value(mockToken));
    }

    @Test
    void 로그인_실패_사용자없음() throws Exception {
        // Given
        when(userService.userLogin(any(LoginRequest.class))).thenThrow(new IllegalArgumentException("Invalid email or password."));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .param("userEmail", "notfound@example.com")
                .param("userPassword", "password")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isBadRequest());
    }
}