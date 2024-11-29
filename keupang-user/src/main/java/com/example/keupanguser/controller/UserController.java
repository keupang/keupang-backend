package com.example.keupanguser.controller;

import com.example.keupanguser.domain.User;
import com.example.keupanguser.request.UserRequest;
import com.example.keupanguser.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public String getUsers(@ModelAttribute UserRequest user) {
        log.debug(user.getUserPassword());
        User user1 = userService.registerUser(user);
        if(user1 == null){
            log.info("회원가입 실패");
            throw new IllegalArgumentException("회원가입 실패");
        }
        return user1.getUserName();
    }
}
