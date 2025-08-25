package com.example.calmall.product.text;

import org.springframework.web.util.HtmlUtils;


// 整形済みHTML → プレーンテキスト変換ユーティリティ
public final class DescriptionHtmlToPlain {

    private DescriptionHtmlToPlain() {}

    public static String toPlain(String html) {
        if (html == null) return "";
        String s = html;

        // <style>と<script> ブロックを丸ごと削除
        s = s.replaceAll("(?is)<style[^>]*>.*?</style>", "");
        s = s.replaceAll("(?is)<script[^>]*>.*?</script>", "");

        // 改行に変換したいタグ
        s = s.replaceAll("(?i)<\\s*br\\s*/?>", "\n");
        s = s.replaceAll("(?i)</\\s*p\\s*>", "\n");
        s = s.replaceAll("(?i)</\\s*li\\s*>", "\n");
        s = s.replaceAll("(?i)</\\s*tr\\s*>", "\n");

        // 残りのタグを削除
        s = s.replaceAll("<[^>]+>", "");

        s = HtmlUtils.htmlUnescape(s);

        s = s.replace("\u00A0", " "); // ノーブレークスペース
        s = s.replaceAll("[ \\t\\x0B\\f\\r　]+", " "); // 半角/全角スペースの連続 → 1個

        // 改行の正規化
        s = s.replaceAll("\\n{2,}", "\n").trim();

        return s;
    }
}
