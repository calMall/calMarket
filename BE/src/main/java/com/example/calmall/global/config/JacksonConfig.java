package com.example.calmall.global.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * 全体の Jackson（JSON シリアライズ）設定を行うクラス
 * - 全 API の LocalDateTime を JST（日本時間）+ 秒単位に統一
 * - ミリ秒・マイクロ秒を除外
 * - null フィールドはレスポンスに含めない
 */
@Configuration
public class JacksonConfig {

    /**
     * Spring Boot で使用される ObjectMapper をカスタマイズ
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Java 8 の日時 API (LocalDateTime など) に対応するモジュールを登録
        mapper.registerModule(new JavaTimeModule());

        // タイムゾーンを JST（Asia/Tokyo）に固定
        mapper.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));

        // 日付時刻フォーマット（秒単位まで、ミリ秒・マイクロ秒なし）
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));

        // null のフィールドは JSON 出力しない設定
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // LocalDateTime のシリアライズ時にタイムスタンプ形式を無効化（フォーマットで出力するため）
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}