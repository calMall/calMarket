package com.example.calmall.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * アプリケーション共通設定クラス
 */
@Configuration
public class AppConfig {

    /**
     * RestTemplateのBeanを登録
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
