package keupang.keupangauth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long userId;
    private String userEmail;
    private String userPassword;
    private String userName;
    private String userPhone;
    private Role role;
}
