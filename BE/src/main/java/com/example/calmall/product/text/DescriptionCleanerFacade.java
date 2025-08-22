package com.example.calmall.product.text;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 商品説明文のクリーン化を統合的に扱う Facade
 * - 今は LLM（Groq）のみ利用。失敗しても fallback しない
 */
@Component
@RequiredArgsConstructor
public class DescriptionCleanerFacade {

    private static final Logger log = LoggerFactory.getLogger(DescriptionCleanerFacade.class);

    private final LlmDescriptionFormatter llmFormatter;

    /**
     * HTML ベースの説明文を整形
     */
    public String buildCleanHtml(String descriptionHtml, String descriptionPlain, String itemCaption) {
        try {
            // 呼び出し前のログ
            log.debug("[Groq LLM] Calling cleanToHtml...");
            log.debug("[Groq LLM] inputHtml={} inputPlain={} itemCaption={}",
                    preview(descriptionHtml), preview(descriptionPlain), preview(itemCaption));

            // LLM呼び出し
            String llm = llmFormatter.cleanToHtml(descriptionHtml, descriptionPlain, itemCaption);

            // 呼び出し後のログ
            if (llm != null) {
                log.debug("[Groq LLM] response length={} preview={}", llm.length(), preview(llm));
            } else {
                log.warn("[Groq LLM] returned null response");
            }

            if (llm != null && !llm.isBlank()) {
                return llm;
            }
            throw new IllegalStateException("[Groq LLM] returned empty response");

        } catch (Exception e) {
            log.error("[Groq LLM failed]", e);
            throw e; // fallbackしない
        }
    }

    /**
     * プレーンテキスト化（AI使わないのでそのまま残す）
     */
    public String toPlain(String html) {
        return DescriptionSuperCleaner.toPlain(html);
    }

    /**
     * 長い文字列を短くプレビューするユーティリティ
     */
    private String preview(String input) {
        if (input == null) return "null";
        return input.length() > 200 ? input.substring(0, 200) + "..." : input;
    }
}
