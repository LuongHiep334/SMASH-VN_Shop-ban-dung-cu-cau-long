package com.smashvn.shop.config;

import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    @Deprecated(since = "4.0.0", forRemoval = true)
    public void postProcessEnvironment(ConfigurableEnvironment environment, org.springframework.boot.SpringApplication application) {
        // Test classpaths must never inspect project or parent .env.
        if (getClass().getClassLoader().getResource("META-INF/smash-test-runtime.marker") != null
                || !environment.getProperty("app.dotenv.enabled", Boolean.class, true)) {
            return;
        }
        Path envPath = locateEnvFile();
        if (envPath == null) return;
        try {
            Map<String, Object> dotenvMap = new HashMap<>();
            Files.lines(envPath).forEach(line -> {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#") && line.contains("=")) {
                    int index = line.indexOf('=');
                    String key = line.substring(0, index).trim();
                    String value = line.substring(index + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }

                    // Only load from .env if the key is not already defined as a System Property or an OS Environment Variable
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        dotenvMap.put(key, value);
                    }
                }
            });

            if (!dotenvMap.isEmpty()) {
                MutablePropertySources propertySources = environment.getPropertySources();
                // Add the .env properties directly after OS Environment Variables (systemEnvironment)
                // This guarantees System Properties and OS Env Vars have higher priority than .env,
                // while .env still overrides application.properties defaults.
                if (propertySources.contains("systemEnvironment")) {
                    propertySources.addAfter("systemEnvironment", new MapPropertySource("dotenvProperties", dotenvMap));
                } else {
                    propertySources.addLast(new MapPropertySource("dotenvProperties", dotenvMap));
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load .env file: " + e.getMessage());
        }
    }

    protected Path locateEnvFile() {
        Path path = Paths.get(".env");
        if (Files.exists(path)) return path;
        path = Paths.get("../.env");
        return Files.exists(path) ? path : null;
    }
}
