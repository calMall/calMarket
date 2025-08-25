package com.example.calmall.global.config;

import com.example.calmall.ai.GroqClient;
import com.example.calmall.product.text.LlmDescriptionFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


//  Groq クライアントの Spring 構成クラス
@Configuration
public class GroqConfig {


    // Groqクライアントを生成
    @Bean
    public GroqClient groqClient(
            @Value("${groq.api.base}") String base,
            @Value("${groq.api.key}") String key,
            @Value("${groq.timeout.ms:20000}") int timeoutMs
    ) {
        return new GroqClient(base, key, timeoutMs);
    }


    // LLM商品説明整形用フォーマッタ
    @Bean
    public LlmDescriptionFormatter llmDescriptionFormatter(
            GroqClient client,
            @Value("${groq.model}") String model
    ) {
        // 推奨値を直接設定
        int maxTokens = 768;
        int parallel = 2;

        return new LlmDescriptionFormatter(client, model, maxTokens, parallel);
    }
}
