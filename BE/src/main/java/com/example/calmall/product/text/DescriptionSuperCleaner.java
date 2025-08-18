package com.example.calmall.product.text;

import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DescriptionSuperCleaner {

    private DescriptionSuperCleaner() {}

    // -------- config --------
    // 是否輸出區塊標題（預設 false）
    private static final boolean SHOW_SECTION_TITLES = false;
    private static final String TITLE_BULLETS = "特長・注意";
    private static final String TITLE_TABLE  = "商品情報";
    // 是否印 debug
    private static final boolean DEBUG = false;

    // -------- patterns --------
    private static final Pattern KV_PATTERN     = Pattern.compile("^\\s*([^：:]{1,40})[：:]+\\s*(.+)$");
    private static final Pattern BULLET_PATTERN = Pattern.compile("^\\s*(?:[・●\\-*•]|\\u2022)\\s*(.+)$");
    private static final Pattern TAG_PATTERN    = Pattern.compile("<[^>]+>");

    private static void debug(String tag, String msg) {
        if (DEBUG) System.out.println("[DESC] " + tag + " :: " + msg);
    }

    /** 逐行轉 HTML：bullet→ul，key:value→table，其餘→p（保序） */
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
        boolean inBody = false; // 聚合連續段落

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            // 1) bullet
            Matcher mb = BULLET_PATTERN.matcher(line);
            if (mb.find()) {
                String item = esc(mb.group(1).trim());
                // 關 table/body
                if (inBody) { out.append("</section>"); inBody = false; }
                if (inTable) { out.append("</table></section>"); inTable = false; }

                // 開 ul
                if (!inUl) {
                    out.append("<section class=\"desc-section bullets\">");
                    if (SHOW_SECTION_TITLES) out.append("<h3>").append(esc(TITLE_BULLETS)).append("</h3>");
                    out.append("<ul>");
                    inUl = true;
                }
                out.append("<li>").append(item).append("</li>");
                continue;
            }

            // 2) key:value
            Matcher mkv = KV_PATTERN.matcher(line);
            if (mkv.find()) {
                String key = mkv.group(1).trim();
                String val = mkv.group(2).trim();
                if (!key.isEmpty() && !val.isEmpty() && !key.equals(val)) {
                    // 關 ul/body
                    if (inUl)   { out.append("</ul></section>"); inUl = false; }
                    if (inBody) { out.append("</section>"); inBody = false; }

                    // 開 table
                    if (!inTable) {
                        out.append("<section class=\"desc-section table\">");
                        if (SHOW_SECTION_TITLES) out.append("<h3>").append(esc(TITLE_TABLE)).append("</h3>");
                        out.append("<table>");
                        inTable = true;
                    }
                    out.append("<tr><th>").append(esc(key)).append("</th><td>")
                            .append(esc(val)).append("</td></tr>");
                    continue;
                }
            }

            // 3) 一般段落
            if (inUl)   { out.append("</ul></section>"); inUl = false; }
            if (inTable){ out.append("</table></section>"); inTable = false; }

            if (!inBody) {
                out.append("<section class=\"desc-section body\">");
                inBody = true;
            }
            out.append("<p>").append(esc(line)).append("</p>");
        }

        // 收尾
        if (inUl)   out.append("</ul></section>");
        if (inTable)out.append("</table></section>");
        if (inBody) out.append("</section>");

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

    // -------- helpers --------
    private static String chooseBase(String html, String plain, String caption) {
        if (html  != null && !html.isBlank())  return stripHtmlKeepBreaks(html);
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
        for (String line : text.split("\\n")) {
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
