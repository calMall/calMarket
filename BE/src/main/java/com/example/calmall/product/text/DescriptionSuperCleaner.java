package com.example.calmall.product.text;

import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DescriptionSuperCleaner {

    private DescriptionSuperCleaner() {}

    // =========================================================
    // 正規表現
    // =========================================================
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern BULLET_HEAD = Pattern.compile("^[\\p{Z}\\t　]*[・●\\-*•◆■□◇▶▷◉◦]\\s*");
    private static final Pattern KV_PATTERN = Pattern.compile("^\\s*([^：:]{1,40})[：:]+\\s*(.+)$");
    private static final Pattern SPACES_MANY = Pattern.compile("[ \\t\\x0B\\f\\r　]+");
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=。|！|!|？|\\?|\\.|、)(?!$)");

    // =========================================================
    // 公開API
    // =========================================================
    public static String buildCleanHtml(String descriptionHtml, String descriptionPlain, String itemCaption) {
        String base = chooseBase(descriptionHtml, descriptionPlain, itemCaption);
        String normalized = normalize(base);

        // 行分割 + 長文 fallback
        List<String> lines = splitToLinesWithFallback(normalized);
        lines = mergeBrokenLines(lines);

        List<String[]> tableRows = new ArrayList<>();
        List<String> bullets = new ArrayList<>();
        List<String> paragraphs = new ArrayList<>();

        for (String ln : lines) {
            String t = ln.trim();
            if (t.isEmpty()) continue;

            // 箇条書き
            if (BULLET_HEAD.matcher(t).find()) {
                bullets.add(t.replaceFirst(BULLET_HEAD.pattern(), "").trim());
                continue;
            }

            // Key:Value
            Matcher mkv = KV_PATTERN.matcher(t);
            if (mkv.find()) {
                String key = mkv.group(1).trim();
                String value = mkv.group(2).trim();
                if (!key.isEmpty() && !value.isEmpty()) {
                    tableRows.add(new String[]{key, value});
                    continue;
                }
            }

            // 段落
            paragraphs.add(t);
        }

        // HTML 構築
        StringBuilder out = new StringBuilder();

        if (!tableRows.isEmpty()) {
            out.append("<section class=\"desc-section table\"><table>");
            for (String[] row : tableRows) {
                out.append("<tr><th>").append(esc(row[0])).append("</th><td>").append(esc(row[1])).append("</td></tr>");
            }
            out.append("</table></section>");
        }

        if (!bullets.isEmpty()) {
            out.append("<section class=\"desc-section bullets\"><ul>");
            for (String b : bullets) {
                out.append("<li>").append(esc(b)).append("</li>");
            }
            out.append("</ul></section>");
        }

        if (!paragraphs.isEmpty()) {
            out.append("<section class=\"desc-section body\">");
            for (String p : paragraphs) {
                out.append("<p>").append(esc(p)).append("</p>");
            }
            out.append("</section>");
        }

        return clampToLastSection(out.toString());
    }

    /**
     * HTML からタグを除去してプレーンテキスト化
     */
    public static String toPlain(String html) {
        if (html == null) return "";
        String s = html;
        s = s.replaceAll("(?i)<\\s*br\\s*/?>", "\n");
        s = s.replaceAll("(?i)</\\s*p\\s*>", "\n");
        s = s.replaceAll("(?i)</\\s*li\\s*>", "\n");
        s = s.replaceAll("(?i)</\\s*tr\\s*>", "\n");
        s = TAG_PATTERN.matcher(s).replaceAll("");
        s = HtmlUtils.htmlUnescape(s);
        s = s.replace("\u00A0", " ");
        s = SPACES_MANY.matcher(s).replaceAll(" ");
        s = s.replaceAll("\\n{2,}", "\n").trim();
        return s;
    }

    // =========================================================
    // 内部ユーティリティ
    // =========================================================
    private static String chooseBase(String html, String plain, String caption) {
        if (html != null && !html.isBlank()) return stripHtmlKeepBreaks(html);
        if (plain != null && !plain.isBlank()) return plain;
        return caption != null ? caption : "";
    }

    private static String stripHtmlKeepBreaks(String html) {
        String s = html;
        s = s.replaceAll("(?i)<\\s*br\\s*/?>", "\n");
        s = s.replaceAll("(?i)</\\s*p\\s*>", "\n");
        s = s.replaceAll("(?i)</\\s*li\\s*>", "\n");
        s = s.replaceAll("(?i)</\\s*tr\\s*>", "\n");
        s = TAG_PATTERN.matcher(s).replaceAll("");
        return s;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String t = HtmlUtils.htmlUnescape(s);
        t = t.replace('\u00A0', ' ').replace('\u3000', ' ');
        t = t.replace("\r\n", "\n").replace("\r", "\n");
        return t.trim();
    }

    private static List<String> splitToLinesWithFallback(String text) {
        List<String> lines = new ArrayList<>();
        String[] raw = text.split("\\n");
        for (String r : raw) {
            String t = r.trim();
            if (t.isEmpty()) continue;
            if (t.length() > 120 && t.indexOf('。') >= 0) {
                for (String s : SENTENCE_SPLIT.split(t)) {
                    String x = s.trim();
                    if (!x.isEmpty()) lines.add(x);
                }
            } else {
                lines.add(t);
            }
        }
        return lines;
    }

    private static List<String> mergeBrokenLines(List<String> lines) {
        List<String> merged = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (String l : lines) {
            if (!l.endsWith("。") && !l.endsWith("」") && !l.endsWith("』") && l.length() < 40) {
                buf.append(l);
            } else {
                if (buf.length() > 0) {
                    buf.append(l);
                    merged.add(buf.toString());
                    buf.setLength(0);
                } else {
                    merged.add(l);
                }
            }
        }
        if (buf.length() > 0) merged.add(buf.toString());
        return merged;
    }

    private static String clampToLastSection(String html) {
        if (html == null) return null;
        int i = html.lastIndexOf("</section>");
        return (i >= 0) ? html.substring(0, i + "</section>".length()) : html;
    }

    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '&' -> sb.append("&amp;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
