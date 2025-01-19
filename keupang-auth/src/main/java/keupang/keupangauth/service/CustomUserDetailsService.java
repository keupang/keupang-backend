package keupang.keupangauth.service;

import keupang.keupangauth.client.UserClient;
import keupang.keupangauth.domain.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserClient userClient;
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userClient.findByUserEmail(email)
            .map(CustomUserDetails::new)
            .orElseThrow(()-> new UsernameNotFoundException("해당 이메일을 가진 유저는 없습니다. 이메일 : "+ email));
    }
}
