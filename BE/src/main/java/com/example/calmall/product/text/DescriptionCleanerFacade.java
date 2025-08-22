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

    /** HTMLベースの説明文を LLM のみで整形 */
    public String buildCleanHtml(String descriptionHtml, String descriptionPlain, String itemCaption) {
        log.debug("[Groq LLM] Calling cleanToHtml...");
        log.debug("[Groq LLM] inputHtml={} inputPlain={} itemCaption={}",
                preview(descriptionHtml), preview(descriptionPlain), preview(itemCaption));

        final String llm = llmFormatter.cleanToHtml(descriptionHtml, descriptionPlain, itemCaption);

        log.debug("[Groq LLM] raw response length={} preview={}",
                (llm != null ? llm.length() : 0), preview(llm));

        return llm;
    }

    /** 互換性維持のため残す。内部で共通ユーティリティに委譲 */
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
