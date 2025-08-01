package com.example.calmall.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Webの全体設定クラス（CORSや静的リソースのマッピングなど）
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // CORSの設定（フロントエンドと連携）
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://cal-market.vercel.app", "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    // アップロードされた画像を外部公開するための設定
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**") // ブラウザからアクセス可能なパス
                .addResourceLocations("file:uploads/"); // 実際のサーバー上のファイルパス
    }
}
