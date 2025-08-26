package com.example.calmall.product.text;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * LLMによる説明文整形の入口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DescriptionCleanerFacade {

    private final LlmDescriptionFormatter llmFormatter;

    /**
     * HTMLベースの説明文を LLM のみで整形
     * @param descriptionHtml 楽天からのHTML説明
     * @param descriptionPlain プレーンテキスト説明
     * @param itemCaption 楽天のキャッチコピー
     * @param itemName 商品名（fallback時に利用）
     */
    public String buildCleanHtml(String descriptionHtml,
                                 String descriptionPlain,
                                 String itemCaption,
                                 String itemName) {
        log.debug("[Groq LLM] Calling cleanToHtml...");
        log.debug("[Groq LLM] inputHtml={} inputPlain={} itemCaption={} itemName={}",
                preview(descriptionHtml), preview(descriptionPlain), preview(itemCaption), preview(itemName));

        final String llm = llmFormatter.cleanToHtml(descriptionHtml, descriptionPlain, itemCaption, itemName);

        log.debug("[Groq LLM] raw response length={} preview={}",
                (llm != null ? llm.length() : 0), preview(llm));

        return llm;
    }

    /** 互換性維持（旧シグネチャ用） */
    public String buildCleanHtml(String descriptionHtml, String descriptionPlain, String itemCaption) {
        return buildCleanHtml(descriptionHtml, descriptionPlain, itemCaption, null);
    }

    /** HTML→プレーン変換ユーティリティ */
    public String toPlain(String html) {
        return DescriptionHtmlToPlain.toPlain(html);
    }

    /** ログ用の短縮表示 */
    private static String preview(String s) {
        if (s == null) return "null";
        final String t = s.replaceAll("\\s+", " ").trim();
        return t.length() > 120 ? (t.substring(0, 110) + "...") : t;
    }
}
