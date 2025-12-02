package keupang.keupangauth.client;

import keupang.keupangauth.dto.UserDto;
import java.util.Optional;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "https://api.keupang.store/api/user")
public interface UserClient {

    @GetMapping("/jwt/{email}")
    Optional<UserDto> findByUserEmail(@PathVariable String email);
}