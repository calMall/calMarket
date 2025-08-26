package com.example.calmall.product.text;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 商品説明文を LLM で整形するためのファサードクラス。
 * - LlmDescriptionFormatter を呼び出す入口として利用する。
 * - 旧コードとの互換性を保つために、3引数版 buildCleanHtml と toPlain を残している。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DescriptionCleanerFacade {

    // LLM を利用した説明文整形クラス
    private final LlmDescriptionFormatter formatter;

    /**
     * 商品説明をクリーンな HTML に整形する（新バージョン：4引数）
     *
     * @param rawHtml     元の HTML 説明文（楽天 API から取得したもの）
     * @param rawPlain    元のプレーンテキスト説明文
     * @param itemCaption 商品キャプション（楽天 API 項目）
     * @param itemName    商品名
     * @return 整形後の HTML 文字列
     */
    public String buildCleanHtml(String rawHtml, String rawPlain, String itemCaption, String itemName) {
        log.debug("[Groq LLM] Calling cleanToHtml...");
        log.debug("[Groq LLM] inputHtml={} inputPlain={} itemCaption={} itemName={}",
                preview(rawHtml), preview(rawPlain), preview(itemCaption), preview(itemName));
        return formatter.cleanToHtml(rawHtml, rawPlain, itemCaption, itemName);
    }

    /**
     * 旧バージョン互換用：3引数の buildCleanHtml。
     * 既存のコード（RakutenApiServiceImpl, BackfillRunner 等）を壊さないために残している。
     * 内部的には 4引数版に委譲し、itemName は null として扱う。
     *
     * @param rawHtml     元の HTML 説明文
     * @param rawPlain    元のプレーンテキスト説明文
     * @param itemCaption 商品キャプション
     * @return 整形後の HTML 文字列
     */
    public String buildCleanHtml(String rawHtml, String rawPlain, String itemCaption) {
        log.debug("[Groq LLM] Calling cleanToHtml (compat 3-args)...");
        log.debug("[Groq LLM] inputHtml={} inputPlain={} itemCaption={}",
                preview(rawHtml), preview(rawPlain), preview(itemCaption));
        return formatter.cleanToHtml(rawHtml, rawPlain, itemCaption, null);
    }

    /**
     * HTML をプレーンテキストに変換するメソッド。
     * 旧コードとの互換性を維持するために公開。
     * 内部では DescriptionHtmlToPlain を利用する。
     *
     * @param html HTML 文字列
     * @return プレーンテキスト
     */
    public String toPlain(String html) {
        return DescriptionHtmlToPlain.toPlain(html);
    }

    // --- 内部ユーティリティ ---
    private static String preview(String s) {
        if (!StringUtils.hasText(s)) return "null";
        String t = s.trim();
        if (t.length() > 60) t = t.substring(0, 60) + "...";
        return t.replace("\n", "\\n");
    }
}
