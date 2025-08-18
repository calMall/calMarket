package com.example.calmall.product.text;

import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DescriptionSuperCleaner {

    private DescriptionSuperCleaner() {}

    private static final Pattern KV_PATTERN     = Pattern.compile("^\\s*([^：:]{1,40})[：:]+\\s*(.+)$");
    private static final Pattern BULLET_PATTERN = Pattern.compile("^\\s*(?:[・●\\-*•]|\\u2022)\\s*(.+)$");
    private static final Pattern TAG_PATTERN    = Pattern.compile("<[^>]+>");

    // 打開時會印除錯訊息到 stdout
    private static final boolean DEBUG = false;
    private static void debug(String tag, String msg) {
        if (DEBUG) System.out.println("[DESC] " + tag + " :: " + msg);
    }

    /** 逐行轉 HTML，保留原本順序；bullet→ul，key:value→table，其餘→p */
    public static String buildCleanHtml(String descriptionHtml, String descriptionPlain, String itemCaption) {
        String base       = chooseBase(descriptionHtml, descriptionPlain, itemCaption);
        String normalized = normalize(base);
        List<String> lines = splitToLines(normalized);

        debug("BASE", base);
        debug("NORMALIZED", normalized);
        debug("LINES", String.join(" || ", lines));

        StringBuilder out = new StringBuilder(normalized.length() + 256);
        boolean inUl = false;
        boolean inTable = false;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            // 1) bullet 行
            Matcher mb = BULLET_PATTERN.matcher(line);
            if (mb.find()) {
                String item = esc(mb.group(1).trim());
                // 關閉 table（若有）
                if (inTable) {
                    out.append("</table></section>");
                    inTable = false;
                }
                // 開啟 ul（若尚未）
                if (!inUl) {
                    out.append("<section class=\"desc-section bullets\"><h3>特長・注意</h3><ul>");
                    inUl = true;
                }
                out.append("<li>").append(item).append("</li>");
                continue;
            }

            // 2) key:value 行
            Matcher mkv = KV_PATTERN.matcher(line);
            if (mkv.find()) {
                String key = mkv.group(1).trim();
                String val = mkv.group(2).trim();
                if (!key.isEmpty() && !val.isEmpty() && !key.equals(val)) {
                    // 關閉 ul（若有）
                    if (inUl) {
                        out.append("</ul></section>");
                        inUl = false;
                    }
                    // 開啟 table（若尚未）
                    if (!inTable) {
                        out.append("<section class=\"desc-section table\"><h3>商品情報</h3><table>");
                        inTable = true;
                    }
                    out.append("<tr><th>").append(esc(key)).append("</th><td>")
                            .append(esc(val)).append("</td></tr>");
                    continue;
                }
            }

            // 3) 一般段落：先關閉任何開著的 ul / table
            if (inUl) {
                out.append("</ul></section>");
                inUl = false;
            }
            if (inTable) {
                out.append("</table></section>");
                inTable = false;
            }
            out.append("<section class=\"desc-section body\"><p>")
                    .append(esc(line))
                    .append("</p></section>");
        }

        // 收尾：若還有未關閉的區塊
        if (inUl) {
            out.append("</ul></section>");
        }
        if (inTable) {
            out.append("</table></section>");
        }

        String html = clampToLastSection(out.toString());
        debug("HTML", html);
        return html;
    }

    /** HTML → Plain（保留換行） */
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
        s = s.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
        s = s.replaceAll("\\n{2,}", "\n").trim();
        return s;
    }

    // ── helpers ────────────────────────────────────────────────────────────────
    private static String chooseBase(String html, String plain, String caption) {
        if (html != null && !html.isBlank())  return stripHtmlKeepBreaks(html);
        if (plain != null && !plain.isBlank()) return plain;
        return caption != null ? caption : "";
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String t = HtmlUtils.htmlUnescape(s);
        t = t.replace("\u00A0", " ");
        t = t.replace('\u3000', ' ');
        t = t.replace("\r\n", "\n").replace("\r", "\n");
        return t.trim();
    }

    private static List<String> splitToLines(String text) {
        List<String> out = new ArrayList<>();
        String[] arr = text.split("\\n");
        for (String line : arr) {
            if (!line.trim().isEmpty()) out.add(line);
        }
        return out;
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
                default  -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
