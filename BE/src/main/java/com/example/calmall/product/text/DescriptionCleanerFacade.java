package com.example.calmall.product.text;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 商品説明文の整形を司る Facade。
 *
 * 【重要変更点】
 * - フォールバック（ローカル整形）は廃止。LLMが失敗/未設定なら例外を投げて fail-fast。
 * - LLM出力が「実質的に原文と同等（=パススルー）」なら例外を投げて fail-fast。
 * - 詳細な DEBUG ログを出力（入力プレビュー/出力プレビュー/失敗理由）。
 */
@Component
@RequiredArgsConstructor
public class DescriptionCleanerFacade {

    private static final Logger log = LoggerFactory.getLogger(DescriptionCleanerFacade.class);

    private final LlmDescriptionFormatter llmFormatter;

    /**
     * HTMLベースの説明文を LLM で構造化HTMLに整形する。
     * @param descriptionHtml 楽天API等からのHTML
     * @param descriptionPlain 同上プレーンテキスト
     * @param itemCaption 商品キャプション（あれば参考）
     * @return 構造化済みの安全なHTML断片
     */
    public String buildCleanHtml(String descriptionHtml, String descriptionPlain, String itemCaption) {
        try {
            log.debug("[Groq LLM] Calling cleanToHtml...");
            log.debug("[Groq LLM] inputHtml={} inputPlain={} itemCaption={}",
                    preview(descriptionHtml), preview(descriptionPlain), preview(itemCaption));

            final String llm = llmFormatter.cleanToHtml(descriptionHtml, descriptionPlain, itemCaption);

            log.debug("[Groq LLM] raw response length={} preview={}",
                    (llm == null ? -1 : llm.length()), preview(llm));

            // 原文パススルー（実質未整形）を検知 → 例外
            if (looksLikePassThrough(llm, descriptionHtml, descriptionPlain, itemCaption)) {
                throw new IllegalStateException("[Groq LLM] appears disabled or returned pass-through content");
            }

            if (llm == null || llm.isBlank()) {
                throw new IllegalStateException("[Groq LLM] returned empty response");
            }

            return llm;

        } catch (Exception e) {
            log.error("[Groq LLM failed] {}", e.toString(), e);
            // フォールバックはしない。明示的に失敗させることで早期に不具合を検知。
            throw e;
        }
    }

    /** プレーン変換（※ここは既存のローカル処理を残す。AIは使わない簡易用途） */
    public String toPlain(String html) {
        return DescriptionSuperCleanerBase.toPlain(html);
    }

    // ---- helpers ----

    /** 長文プレビュー用（200文字で丸め） */
    private String preview(String s) {
        if (s == null) return "null";
        return (s.length() > 200) ? s.substring(0, 200) + "..." : s;
    }

    /**
     * LLM出力が原文の事実上の通し（パススルー）かを簡易判定。
     * 完全一致ではなく、「空白やタグを除けばほぼ含有している」場合もNGとする。
     */
    private boolean looksLikePassThrough(String out, String html, String plain, String caption) {
        if (out == null) return true;
        final String normOut = out.replaceAll("\\s+", "");
        final String a = (html == null ? "" : html).replaceAll("\\s+", "");
        final String b = (plain == null ? "" : plain).replaceAll("\\s+", "");
        final String c = (caption == null ? "" : caption).replaceAll("\\s+", "");
        // どれか一つでも「ほぼ包含」していればパススルーとみなす
        return (!a.isEmpty() && normOut.contains(a))
                || (!b.isEmpty() && normOut.contains(b))
                || (!c.isEmpty() && normOut.contains(c));
    }
}
