package com.example.keupanguser.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    private String userEmail;
    private String userPassword;
}
