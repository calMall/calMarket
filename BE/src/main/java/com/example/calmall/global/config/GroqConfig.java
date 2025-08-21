package com.example.calmall.global.config;

import com.example.calmall.ai.GroqClient;
import com.example.calmall.product.text.LlmDescriptionFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Groq クライアントの Spring 構成。
 * - API Key は環境変数 GROQ_API_KEY または application.properties から取得
 */
@Configuration
public class GroqConfig {

    @Bean
    public GroqClient groqClient(
            @Value("${groq.api.base}") String base,
            @Value("${groq.api.key}") String key,
            @Value("${groq.timeout.ms:20000}") int timeoutMs
    ) {
        return new GroqClient(base, key, timeoutMs);
    }

    @Bean
    public LlmDescriptionFormatter llmDescriptionFormatter(
            GroqClient client,
            @Value("${groq.model}") String model,
            @Value("${groq.max.tokens:1024}") int maxTokens,
            @Value("${groq.max.parallel:2}") int parallel
    ) {
        return new LlmDescriptionFormatter(client, model, maxTokens, parallel);
    }
}
