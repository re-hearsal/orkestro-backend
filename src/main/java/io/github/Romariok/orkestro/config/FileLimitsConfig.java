package io.github.Romariok.orkestro.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FileLimitsProperties.class)
public class FileLimitsConfig {
}
