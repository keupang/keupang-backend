package com.example.keupanguser.request;

import com.example.keupanguser.domain.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequest {
    private String userEmail;
    private String userPassword;
    private String userName;
    private String userPhone;
    private Role role;
}
