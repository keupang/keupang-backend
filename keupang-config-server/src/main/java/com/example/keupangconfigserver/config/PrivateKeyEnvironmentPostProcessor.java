package com.example.keupangconfigserver.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class PrivateKeyEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_NAME = "private_key";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String privateKey = environment.getProperty(PROPERTY_NAME);

        if (privateKey == null || privateKey.isBlank()) {
            return;
        }

        environment.getPropertySources().addFirst(
            new MapPropertySource(
                "decodedConfigServerPrivateKey",
                Map.of(PROPERTY_NAME, decodeIfBase64(privateKey))
            )
        );
    }

    private String decodeIfBase64(String value) {
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return value;
        }
    }
}
