package keupang.keupangauth.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import keupang.keupangauth.utils.PemUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtTokenProvider {

    private KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.ES256);

    @PostConstruct
    public void initKeyPair(){
        try {
            String privateKeyPem = System.getenv("JWT_PRIVATE_KEY");
            String publicKeyPem = System.getenv("JWT_PUBLIC_KEY");

            PrivateKey privateKey = PemUtils.loadPrivateKey(privateKeyPem, "EC");
            PublicKey publicKey = PemUtils.loadPublicKey(publicKeyPem, "EC");

            this.keyPair = new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load key pair", e);
        }
    }

    public String createToken(String email, String role) {
        Date now = new Date();
        long validityInMilliseconds = 360000;
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
            .setSubject(email)
            .claim("role", role)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(keyPair.getPrivate())
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(keyPair.getPublic())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("이상한 토큰 이잖아 저리가", e);
            return false;
        }
    }

    public String getEmail(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(keyPair.getPublic())
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }

    public String getRole(String token){
        return (String) Jwts.parserBuilder()
            .setSigningKey(keyPair.getPublic())
            .build()
            .parseClaimsJws(token)
            .getBody()
            .get("role");
    }

}
