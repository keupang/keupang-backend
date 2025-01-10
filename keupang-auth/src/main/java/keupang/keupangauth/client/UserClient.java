package keupang.keupangauth.client;

import com.example.keupanguser.domain.User;
import java.util.Optional;
import keupang.keupangauth.response.UserDetailsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "https://api.keupang.store")
public interface UserClient {

}