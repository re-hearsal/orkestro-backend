package io.github.Romariok.orkestro.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "orkestro.limits")
public class FileLimitsProperties {
    private int eventMaxFiles;
    private int taskMaxFiles;
    private int songMaxFiles;
    private int linksTypes;
}
