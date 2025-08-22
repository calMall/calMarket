package com.example.calmall.product.text;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


//商品説明文の整形をLLMにするFacade
@Slf4j
@Component
@RequiredArgsConstructor
public class DescriptionCleanerFacade {

    private final LlmDescriptionFormatter llmFormatter;


    // HTMLベースの説明文を「LLMのみ」で整形して返す
    public String buildCleanHtml(String descriptionHtml, String descriptionPlain, String itemCaption) {
        log.debug("[Groq LLM] Calling cleanToHtml...");
        log.debug("[Groq LLM] inputHtml={} inputPlain={} itemCaption={}",
                preview(descriptionHtml), preview(descriptionPlain), preview(itemCaption));

        final String llm = llmFormatter.cleanToHtml(descriptionHtml, descriptionPlain, itemCaption);

        log.debug("[Groq LLM] raw response length={} preview={}",
                (llm != null ? llm.length() : 0), preview(llm));

        return llm;
    }

    // LLM出力をそのまま返す方針なので、toPlain も使用しない前提（呼び出し元で不要なら削除可）
    public String toPlain(String html) {
        return html == null ? "" : html.replaceAll("<[^>]+>", "").trim();
    }

    private static String preview(String s) {
        if (s == null) return "null";
        final String t = s.replaceAll("\\s+", " ").trim();
        return t.length() > 120 ? (t.substring(0, 110) + "...") : t;
    }
}
