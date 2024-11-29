package com.example.keupanguser;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "spring.config.name=application-test"
})
@ActiveProfiles("test")
class KeupangUserApplicationTests {

    @Test
    void contextLoads() {
    }

}
