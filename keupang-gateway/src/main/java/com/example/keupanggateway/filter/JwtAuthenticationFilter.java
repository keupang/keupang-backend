package com.example.keupanggateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class JwtAuthenticationFilter implements WebFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    //사용자 정보를 저장할 threadLocal
    private static final ThreadLocal<Map<String, String>> userContext = new ThreadLocal<>();

    @Override
    @NonNull
    public Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String authorizationHeader = exchange.getRequest().getHeaders()
            .getFirst(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authorizationHeader.substring(7); //Bearer 제거
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecret.getBytes()).build()
                .parseClaimsJws(token)
                .getBody();

            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);

            Map<String, String> userDetails = new HashMap<>();
            userDetails.put("userEmail", email);
            userDetails.put("userRole", role);
            userContext.set(userDetails);
        } catch (Exception ex) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange)
            .doFinally(signalType -> userContext.remove());
    }

    // ThreadLocal 에서 사용자 정보 가져오기
    public static Map<String, String> getUserContext() {
        return userContext.get();
    }
}
