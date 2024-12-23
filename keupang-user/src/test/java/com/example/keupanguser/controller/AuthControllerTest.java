package com.example.keupanguser.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.keupanguser.domain.Role;
import com.example.keupanguser.domain.User;
import com.example.keupanguser.repository.UserRepository;
import com.example.keupanguser.request.LoginRequest;
import com.example.keupanguser.request.UserRequest;
import com.example.keupanguser.service.UserService;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = {
    "spring.config.name=application-test"
})
class AuthControllerTest {

    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void 로그인_성공() throws Exception {
        // Given
        String mockToken = "mockedJwtToken";
        LoginRequest loginRequest = new LoginRequest("cj855695@gmail.com", "1234");

        when(userService.userLogin(any(LoginRequest.class))).thenReturn(mockToken);

        // 디버그: Mock 설정 d확인
        System.out.println("Mock 설정 확인: " + userService.userLogin(loginRequest));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .param("userEmail", "cj855695@gmail.com")
                .param("userPassword", "1234")
                .contentType("application/x-www-form-urlencoded"))
            .andExpect(status().isOk())
            .andExpect(result ->
                assertEquals("mockedJwtToken", result.getResponse().getContentAsString()) // 반환값 검증
            );
    }

    @Test
    void 로그인_실패() throws Exception {
        // Given
        when(userService.userLogin(any(LoginRequest.class)))
            .thenThrow(new IllegalArgumentException("Invalid email or password."));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .param("userEmail", "invalid@example.com")
                .param("userPassword", "wrongpassword")
                .contentType("application/x-www-form-urlencoded"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid email or password.")); // JSON 응답 검증

    }
}