package com.sallyvnge.aipromptbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.memory")
@Getter @Setter
public class AppMemoryProperties {
    private boolean enabled = true;

    private Summary summary = new Summary();

    @Getter @Setter
    public static class Summary {
        private int maxTokens = 800;
        private int compressAboveTokens = 1200;
        private int tailMessages = 6;
    }
}
