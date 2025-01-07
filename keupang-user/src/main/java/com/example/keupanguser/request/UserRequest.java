package com.example.keupanguser.request;

import com.example.keupanguser.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequest {
    @Schema(description = "이메일", type = "integer")
    private String userEmail;
    @Schema(description = "비번", type = "integer")
    private String userPassword;
    @Schema(description = "이름", type = "integer")
    private String userName;
    @Schema(description = "전화번호", type = "integer")
    private String userPhone;
}
