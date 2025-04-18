package com.example.keupanguser.request;

import com.example.keupanguser.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springdoc.core.annotations.ParameterObject;

@Getter
@Setter
@ParameterObject
public class UserRequest {
    @Schema(description = "이메일", type = "string")
    private String userEmail;
    @Schema(description = "비번", type = "string")
    private String userPassword;
    @Schema(description = "이름", type = "string")
    private String userName;
    @Schema(description = "전화번호", type = "string")
    private String userPhone;
}
