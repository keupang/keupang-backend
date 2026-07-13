package keupang.keupangauth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import keupang.keupangauth.config.JwtProperties;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createTokenContainsValidRoleClaim() throws Exception {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(new JwtProperties());

        String token = jwtTokenProvider.createToken("user@keupang.local", "USER");
        String payloadJson = new String(
            Base64.getUrlDecoder().decode(token.split("\\.")[1]),
            StandardCharsets.UTF_8
        );
        Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<>() {
        });

        assertThat(payload.get("sub")).isEqualTo("user@keupang.local");
        assertThat(payload.get("role")).isEqualTo("USER");
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getRole(token)).isEqualTo("USER");
    }
}
