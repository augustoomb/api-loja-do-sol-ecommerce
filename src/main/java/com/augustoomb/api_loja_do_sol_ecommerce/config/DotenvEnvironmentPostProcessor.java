package com.augustoomb.api_loja_do_sol_ecommerce.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DOTENV_FILE = ".env";
    private static final String PROPERTY_SOURCE_NAME = "dotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path path = Path.of(DOTENV_FILE);

        if (!Files.exists(path)) {
            return;
        }

        Map<String, Object> properties = new HashMap<>();

        try {
            for (String rawLine : Files.readAllLines(path)) {
                String line = rawLine.trim();

                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }

                int separatorIndex = line.indexOf('=');
                String key = line.substring(0, separatorIndex).trim();
                String value = line.substring(separatorIndex + 1).trim();

                if (!key.isEmpty()) {
                    properties.put(key, value);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler o arquivo " + DOTENV_FILE, e);
        }

        environment.getPropertySources().addAfter("systemEnvironment", new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }
}
