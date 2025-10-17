package com.sallyvnge.aipromptbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.ai.model")
@Getter @Setter
public class AppAiModelProperties {
    private String summary = "gpt-5-nano";
    private String embedding = "text-embedding-3-small"; // 1536 dims
}
