package com.example.calmall.global.config;

import com.example.calmall.ai.GroqClient;
import com.example.calmall.product.text.LlmDescriptionFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GroqConfig {

    @Bean
    public GroqClient groqClient(
            @Value("${groq.base:https://api.groq.com/openai/v1}") String base,
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.timeout.ms:20000}") int timeoutMs
    ) {
        return new GroqClient(base, apiKey, timeoutMs);
    }

    @Bean
    public LlmDescriptionFormatter llmDescriptionFormatter(
            GroqClient client,
            @Value("${groq.model:llama-3.3-70b-versatile}") String model,
            @Value("${groq.max.tokens:1024}") int maxTokens,
            @Value("${groq.parallelism:2}") int parallel
    ) {

        return new LlmDescriptionFormatter(client, model, maxTokens, Math.max(1, parallel));
        // return new LlmDescriptionFormatter(client, model, maxTokens);
    }
}
