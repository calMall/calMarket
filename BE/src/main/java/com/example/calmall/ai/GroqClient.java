package com.example.calmall.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Groq の OpenAI 互換 /chat/completions を叩く最小クライアント。
 * - OkHttp + Jackson
 * - 最初の choice の content を文字列で返すだけの薄い実装
 */
public class GroqClient {

    private final OkHttpClient http;
    private final ObjectMapper om;
    private final String apiKey;
    private final String base;

    public GroqClient(String base, String apiKey, int timeoutMs) {
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        this.apiKey = apiKey;

        this.om = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        this.http = new OkHttpClient.Builder()
                .callTimeout(Duration.ofMillis(timeoutMs))
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * LLMに問い合わせ
     *
     * @param model     Groqモデル名
     * @param messages  system/user メッセージ列
     * @param maxTokens 最大トークン
     * @return 最初の choice.content
     * @throws IOException 通信 or パース失敗時
     */
    public String chat(String model, List<Message> messages, Integer maxTokens) throws IOException {
        ChatRequest req = new ChatRequest(model, messages, maxTokens);

        Request httpReq = new Request.Builder()
                .url(base + "/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(om.writeValueAsBytes(req), MediaType.parse("application/json")))
                .build();

        try (Response resp = http.newCall(httpReq).execute()) {
            if (!resp.isSuccessful()) {
                String errBody = (resp.body() != null) ? resp.body().string() : "";
                throw new IOException("Groq HTTP " + resp.code() + " - " + errBody);
            }

            try (ResponseBody body = resp.body()) {
                if (body == null) throw new IOException("Groq response body is null");
                ChatResponse cr = om.readValue(body.bytes(), ChatResponse.class);

                if (cr.choices == null || cr.choices.isEmpty()) {
                    throw new IOException("Groq response has no choices");
                }
                ChatResponse.Choice choice = cr.choices.get(0);
                if (choice.message == null || choice.message.content == null || choice.message.content.isBlank()) {
                    throw new IOException("Groq response has empty message content");
                }
                return choice.message.content.trim();
            }
        }
    }

    // --- 型 ---
    public record Message(String role, String content) {
        public static Message sys(String c) {
            return new Message("system", c);
        }
        public static Message user(String c) {
            return new Message("user", c);
        }
    }

    public static class ChatRequest {
        public String model;
        public List<Message> messages;
        public Integer max_tokens;
        public Double temperature = 0.0; // 決定論
        public Double top_p = 0.0;       // 可能なら 0 に（互換 OK）

        public ChatRequest() {}
        public ChatRequest(String model, List<Message> messages, Integer maxTokens) {
            this.model = model;
            this.messages = messages;
            this.max_tokens = maxTokens;
        }
    }

    public static class ChatResponse {
        public java.util.List<Choice> choices;
        public static class Choice { public Message message; }
        public static class Message { public String role; public String content; }
    }
}
