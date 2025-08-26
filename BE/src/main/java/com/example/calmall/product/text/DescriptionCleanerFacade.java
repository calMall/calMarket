package com.example.calmall.product.text;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** LLMによる説明文整形の入口 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DescriptionCleanerFacade {

    private final LlmDescriptionFormatter llmFormatter;

    /** 新：429対策で itemName を渡せる 4 引数版 */
    public String buildCleanHtml(String descriptionHtml, String descriptionPlain, String itemCaption, String itemName) {
        log.debug("[Groq LLM] Calling cleanToHtml(4args)...");
        log.debug("[Groq LLM] inputHtml={} inputPlain={} itemCaption={} itemName={}",
                preview(descriptionHtml), preview(descriptionPlain), preview(itemCaption), preview(itemName));
        final String llm = llmFormatter.cleanToHtml(descriptionHtml, descriptionPlain, itemCaption, itemName);
        log.debug("[Groq LLM] raw response length={} preview={}",
                (llm != null ? llm.length() : 0), preview(llm));
        return llm;
    }

    /** 旧：互換維持のため残す（内部で itemName=null を渡す） */
    @Deprecated
    public String buildCleanHtml(String descriptionHtml, String descriptionPlain, String itemCaption) {
        log.debug("[Groq LLM] Calling cleanToHtml(3args, deprecated)...");
        return llmFormatter.cleanToHtml(descriptionHtml, descriptionPlain, itemCaption, null);
    }

    /** HTML → プレーンの補助（既存そのまま） */
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
