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

        // Formatter 側で一次整形
        final String llm = llmFormatter.cleanToHtml(descriptionHtml, descriptionPlain, itemCaption, itemName);

        // Facade 側でも二重防御としてフィルタリング
        final String cleaned = filterPlaceholder(llm);

        log.debug("[Groq LLM] raw response length={} preview={}",
                (cleaned != null ? cleaned.length() : 0), preview(cleaned));

        return cleaned;
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

    /**
     * プレースホルダー除去ユーティリティ（二重防御用）
     * - 「商品の詳細情報はありません。」除去
     * - 「商品説明は表示できません。」除去
     * - 単独行の「商品の」除去
     */
    private static String filterPlaceholder(String text) {
        if (text == null) return null;
        String cleaned = text;

        // 「商品の詳細情報はありません。」の除去
        cleaned = cleaned.replaceAll("(<p>\\s*)?商品の詳細情報はありません。(<\\/p>)?", "");

        // 「商品説明は表示できません。」の除去
        cleaned = cleaned.replaceAll("(<p>\\s*)?商品説明は表示できません。.*?(<\\/p>)?", "");

        // 単独の「商品の」を除去（前後換行あり/なし両方対応）
        cleaned = cleaned.replaceAll("(?m)^\\s*商品の?\\s*$", "");

        return cleaned.trim();
    }
}
