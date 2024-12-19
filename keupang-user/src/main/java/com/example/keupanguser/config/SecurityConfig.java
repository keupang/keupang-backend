package com.example.keupanguser.config;

import com.example.keupanguser.jwt.JwtTokenFilter;
import com.example.keupanguser.jwt.JwtTokenProvider;
import com.example.keupanguser.repository.UserRepository;
import com.example.keupanguser.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception{
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtTokenFilter jwtTokenFilter() {
        return new JwtTokenFilter(jwtTokenProvider, userDetailsService());
    }

    @Bean
    public UserDetailsService userDetailsService(){
        return new CustomUserDetailsService(userRepository);
    }

    //비번 암호화용도
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    //필터 체인
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests((authorizeHttpRequests) -> authorizeHttpRequests
                .requestMatchers("/api/user/**", "/api/auth/**", "/user/**")
                .permitAll()  // 로그인, 회원가입 엔드포인트 허용
                .anyRequest().authenticated())
            .formLogin(AbstractHttpConfigurer::disable)
            .addFilterBefore(jwtTokenFilter(), UsernamePasswordAuthenticationFilter.class)
        ;
        return http.build();
    }
}
