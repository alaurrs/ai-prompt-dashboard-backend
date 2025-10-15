package com.sallyvnge.aipromptbackend.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OpenAiConfig {

    @Bean
    OpenAIClient openAIClient(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${openai.org-id:}") String orgId,
            @Value("${openai.project-id:}") String projectId,
            @Value("${openai.timeout-seconds:0}") long timeoutSeconds
    ) {
        return OpenAIOkHttpClient.builder()
                .fromEnv()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .organization(orgId.isBlank() ? null : orgId)
                .project(projectId.isBlank() ? null : projectId)
                .timeout(timeoutSeconds <= 0 ? Duration.ZERO : Duration.ofSeconds(timeoutSeconds))
                .build();
    }
}
