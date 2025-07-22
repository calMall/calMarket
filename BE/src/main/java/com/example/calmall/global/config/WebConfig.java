package com.example.calmall.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Webの全体設定クラス（CORSなど）
 * このクラスでは、Vercelフロントエンドからのリクエストを許可するCORS設定を行う。
 */
@Configuration // Springに設定クラスとして認識させる
public class WebConfig implements WebMvcConfigurer {

    /**
     * CORS（クロスオリジンリソース共有）の設定
     * フロントエンドの cal-market.vercel.app からのアクセスを許可
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // "/api/"で始まるすべてのAPIを対象とする
                .allowedOrigins("https://cal-market.vercel.app", "http://localhost:3000") // 許可するフロントエンドのURL
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // 許可するHTTPメソッド
                .allowedHeaders("*") // すべてのリクエストヘッダーを許可
                .allowCredentials(true); // Cookie（セッション）の使用を許可
    }
}
