package com.example.calmall.product.text;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class DescriptionCleanerFacade {

    private final LlmDescriptionFormatter formatter;

    private static final String GROQ_FALLBACK_MARKER = "<!--__GROQ_FALLBACK__-->";
    private static final String GROQ_FALLBACK_PHRASE = "Groq の1日あたりのトークン上限を超過しました";

    private static boolean hasFallbackFlag(String s) {
        return s != null && (s.contains(GROQ_FALLBACK_MARKER) || s.contains(GROQ_FALLBACK_PHRASE));
    }

    private static String minimalFallbackHtml(String itemName) {
        String msg = (StringUtils.hasText(itemName))
                ? itemName + " の商品説明は現在表示できません。（LLM利用上限）"
                : "商品説明は現在表示できません。（LLM利用上限）";
        // 不要回傳 marker，避免外洩至前端/DB
        return "<section class=\"desc-section body\"><p>" + msg + "</p></section>";
    }

    public String buildCleanHtml(String inputHtml, String inputPlain, String itemCaption, String itemName) {
        log.debug("[Groq LLM] Calling cleanToHtml...");
        log.debug("[Groq LLM] inputHtml={} inputPlain={} itemCaption={} itemName={}",
                preview(inputHtml), preview(inputPlain), preview(itemCaption), preview(itemName));

        // 任一欄含 fallback → 短路，不打 LLM
        if (hasFallbackFlag(inputHtml) || hasFallbackFlag(inputPlain) || hasFallbackFlag(itemCaption)) {
            log.warn("[Groq LLM] detected fallback flag in inputs → short-circuit w/ minimal html");
            return minimalFallbackHtml(itemName);
        }

        return formatter.cleanToHtml(inputHtml, inputPlain, itemCaption, itemName);
    }

    private static String preview(String s) {
        if (s == null) return "null";
        String t = s.replaceAll("\\s+", " ");
        return (t.length() > 60) ? t.substring(0, 60) + "..." : t;
    }
}
