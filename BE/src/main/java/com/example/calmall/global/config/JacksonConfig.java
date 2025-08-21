package com.example.calmall.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Jackson の全域設定クラス
 * - LocalDateTime を "yyyy-MM-dd HH:mm:ss"（JST・秒精度・ミリ秒なし）でシリアライズ
 * - 全ての LocalDateTime に適用されるため、各 Service/DTO 側で手動変換は不要
 */
@Configuration
public class JacksonConfig {

    /** LocalDateTime 用の共通フォーマッタ（JST・秒精度） */
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * ObjectMapper の Bean 設定
     * @return 設定済み ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 全域時區設定（JST）
        mapper.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));

        // JavaTimeModule を作成し、LocalDateTime 専用シリアライザを登録
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(java.time.LocalDateTime.class, new LocalDateTimeSerializer(FORMATTER));
        mapper.registerModule(javaTimeModule);

        // タイムスタンプ（数値）ではなく文字列で出力する設定
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}
