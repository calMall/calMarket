package com.example.calmall.product.text;

import org.springframework.web.util.HtmlUtils;

/**
 * 整形済みHTML → プレーンテキスト変換
 */
public final class DescriptionHtmlToPlain {

    private DescriptionHtmlToPlain(){}

    public static String toPlain(String html) {
        if (html == null) return "";
        String s = html;
        s = s.replaceAll("(?i)<\\s*br\\s*/?>", "\n");
        s = s.replaceAll("(?i)</\\s*p\\s*>", "\n");
        s = s.replaceAll("(?i)</\\s*li\\s*>", "\n");
        s = s.replaceAll("(?i)</\\s*tr\\s*>", "\n");
        s = s.replaceAll("<[^>]+>", "");
        s = HtmlUtils.htmlUnescape(s);
        s = s.replace("\u00A0", " ");
        s = s.replaceAll("[ \\t\\x0B\\f\\r　]+", " ");
        s = s.replaceAll("\\n{2,}", "\n").trim();
        return s;
    }
}
