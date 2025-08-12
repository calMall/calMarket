package com.example.calmall.product.text;

import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 可読なHTML断片（p, ul/li, table）に変換するユーティリティ
// 外部ライブラリ（Jsoup等）に依存せず、入力は必ずエスケープしてから必要なタグだけを生成する方式にして安全性を担保
public final class DescriptionHtmlFormatter {

    private static final Pattern KEY_VALUE = Pattern.compile("^\\s*([^：:]{1,30})[：:][\\s　]*(.+)$");
    private static final Pattern BULLET_HEAD = Pattern.compile("^[\\p{Z}\\t　]*[・\\-\\—\\●\\■\\□\\*]\\s*");

    private DescriptionHtmlFormatter() {}

    // 生テキストを HTML に整形（安全なタグのみ生成）
    public static String toSafeHtml(String raw) {
        if (raw == null || raw.isBlank()) return "";

        // まずはプレーンテキストへ（改行や箇条書き/キー:値を判定しやすくする）
        String plain = JpTextQuickFormat.toReadablePlain(raw);

        // 行に分割
        List<String> lines = Arrays.asList(plain.split("\\n"));

        // 収集
        List<String[]> tableRows = new ArrayList<>(); // {key, value}
        List<String> bullets = new ArrayList<>();
        List<String> paragraphs = new ArrayList<>();

        for (String ln : lines) {
            String t = ln.trim();
            if (t.isEmpty()) continue;

            Matcher kv = KEY_VALUE.matcher(t);
            if (kv.find()) {
                tableRows.add(new String[]{kv.group(1).trim(), kv.group(2).trim()});
                continue;
            }
            if (t.startsWith("・")) {
                bullets.add(t.substring(1).trim());
                continue;
            }
            paragraphs.add(t);
        }

        // HTML構築（必ずエスケープしてからタグを生成）
        StringBuilder html = new StringBuilder(1024);

        if (!tableRows.isEmpty()) {
            html.append("<section class=\"desc-section table\"><h3>商品情報</h3><table>");
            for (String[] row : tableRows) {
                html.append("<tr><th>").append(escape(row[0])).append("</th><td>")
                        .append(escape(row[1])).append("</td></tr>");
            }
            html.append("</table></section>");
        }

        if (!bullets.isEmpty()) {
            html.append("<section class=\"desc-section bullets\"><h3>特長・注意</h3><ul>");
            for (String b : bullets) {
                html.append("<li>").append(escape(b)).append("</li>");
            }
            html.append("</ul></section>");
        }

        if (!paragraphs.isEmpty()) {
            html.append("<section class=\"desc-section body\">");
            for (String p : paragraphs) {
                html.append("<p>").append(escape(p)).append("</p>");
            }
            html.append("</section>");
        }

        return html.toString();
    }

    // 必要最小限のエスケープ
    private static String escape(String s) {
        return HtmlUtils.htmlEscape(s == null ? "" : s);
    }
}
