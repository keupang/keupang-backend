package keupang.keupangauth.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtTokenProvider {

    private final KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.ES256);
    private final PrivateKey privateKey = keyPair.getPrivate();
    private final PublicKey publicKey = keyPair.getPublic();

    public String createToken(String email, String role) {
        Date now = new Date();
        long validityInMilliseconds = 360000;
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
            .setSubject(email)
            .claim("role", role)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(privateKey)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("이상한 토큰 이잖아 저리가", e);
            return false;
        }
    }

    public String getEmail(String token) {
        return Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token).getBody().getSubject();
    }

    public String getRole(String token){
        return (String) Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token).getBody().get("role");
    }

}
