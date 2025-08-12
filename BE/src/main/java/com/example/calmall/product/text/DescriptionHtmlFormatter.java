package com.example.calmall.product.text;

import org.springframework.web.util.HtmlUtils;

/** 説明文→安全なHTML（見出し/箇条書き/段落に整形） */
public final class DescriptionHtmlFormatter {
    private DescriptionHtmlFormatter() {}

    public static String toSafeHtml(String raw){
        String plain = raw == null ? "" : raw;

        // 讀みやすく整形（全角空白→半角、連続空白圧縮、記号正規化、句読点で改行）
        plain = JpTextQuickFormat.toReadablePlain(plain);

        // 【見出し】を暫定マーカーに
        plain = plain.replaceAll("【([^】]{2,60})】", "\n§$1\n");

        StringBuilder sb = new StringBuilder();
        boolean inList = false;

        for (String line : plain.split("\\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("§")) { // 見出し
                if (inList) { sb.append("</ul>"); inList = false; }
                sb.append("<h4>")
                        .append(HtmlUtils.htmlEscape(line.substring(1)))
                        .append("</h4>");
                continue;
            }
            if (line.startsWith("・")) { // 箇条書き
                if (!inList) { sb.append("<ul>"); inList = true; }
                sb.append("<li>")
                        .append(HtmlUtils.htmlEscape(line.substring(1).trim()))
                        .append("</li>");
            } else {                    // 段落
                if (inList) { sb.append("</ul>"); inList = false; }
                sb.append("<p>")
                        .append(HtmlUtils.htmlEscape(line))
                        .append("</p>");
            }
        }
        if (inList) sb.append("</ul>");
        return sb.toString();
    }
}
