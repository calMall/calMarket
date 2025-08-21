package com.example.calmall.product.text;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 商品説明文のクリーン化を統合的に扱う Facade
 * - まず LLM（Groq）で試し、失敗したら SuperCleaner へフォールバック
 */
@Component
@RequiredArgsConstructor
public class DescriptionCleanerFacade {

    private final LlmDescriptionFormatter llmFormatter;

    /**
     * HTML ベースの説明文を整形
     */
    public String buildCleanHtml(String descriptionHtml, String descriptionPlain, String itemCaption) {
        try {
            // ★ LLM呼び出し（baseではなく3引数で渡す）
            String llm = llmFormatter.cleanToHtml(descriptionHtml, descriptionPlain, itemCaption);
            if (llm != null && !llm.isBlank()) {
                return llm;
            }
        } catch (Exception e) {
            // LLM失敗時はログ残して fallback
            System.err.println("[Groq LLM failed] " + e.getMessage());
        }

        // ★ fallback
        return DescriptionSuperCleaner.buildCleanHtml(descriptionHtml, descriptionPlain, itemCaption);
    }

    /**
     * プレーンテキスト化
     */
    public String toPlain(String html) {
        return DescriptionSuperCleaner.toPlain(html);
    }
}
