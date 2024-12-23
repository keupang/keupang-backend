package com.example.keupanguser.controller;

import com.example.keupanguser.domain.User;
import com.example.keupanguser.exception.CustomException;
import com.example.keupanguser.request.UserRequest;
import com.example.keupanguser.service.UserService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;
    @PostMapping("/users")
    public ResponseEntity<?> getUsers(@ModelAttribute UserRequest userRequest) {
        log.debug(userRequest.getUserPassword());
        User user1 = userService.registerUser(userRequest);
        if(user1 == null){
            log.info("회원가입 실패");
            throw new CustomException(
                HttpStatus.BAD_REQUEST,
                40000,
                "회원가입 과정에서 문제가 발생했습니다.",
                "기입된 정보를 다시 확인하고 시도해주세요.",
                null
            );
        }
        // "data" 필드 값 추가
        Map<String, Object> data = new HashMap<>();
        data.put("name", user1.getUserName());

        // 응답 생성
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", 201);
        responseBody.put("code", 20000);
        responseBody.put("message", "로그인에 성공 했습니다.");
        responseBody.put("data", data); // "data" 필드 추가

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }
}
