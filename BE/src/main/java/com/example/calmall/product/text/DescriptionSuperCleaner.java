package com.example.calmall.product.text;

import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DescriptionSuperCleaner {

    private DescriptionSuperCleaner() {}

    private static final Pattern KV_PATTERN = Pattern.compile("^\\s*([^：:]{1,20})[：:]+\\s*(.+)$");
    private static final Pattern BULLET_PATTERN = Pattern.compile("^\\s*(?:[・●\\-*•]|\\u2022)\\s*(.+)$");
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");

    private static final String[] NOISE_PHRASES = {};

    // 🔹 Debug flag
    private static final boolean DEBUG = true;

    public static String buildCleanHtml(String descriptionHtml, String descriptionPlain, String itemCaption) {
        String base = chooseBase(descriptionHtml, descriptionPlain, itemCaption);
        debug("BASE", base);

        String normalized = normalize(base);
        debug("NORMALIZED", normalized);

        List<String> lines = splitToLines(normalized);
        debug("LINES", String.join(" || ", lines));

        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String ln : lines) {
            String t = ln.trim();
            if (t.isEmpty()) continue;
            set.add(t);
        }
        List<String> clean = new ArrayList<>(set);
        debug("CLEAN", String.join(" || ", clean));

        List<KV> kvs = new ArrayList<>();
        List<String> bullets = new ArrayList<>();
        List<String> paras = new ArrayList<>();

        for (String t : clean) {
            Matcher mkv = KV_PATTERN.matcher(t);
            if (mkv.find()) {
                String key = mkv.group(1).trim();
                String val = mkv.group(2).trim();
                if (!key.isEmpty() && !val.isEmpty() && !key.equals(val)) {
                    kvs.add(new KV(key, val));
                    continue;
                }
            }
            Matcher mb = BULLET_PATTERN.matcher(t);
            if (mb.find()) {
                bullets.add(mb.group(1).trim());
                continue;
            }
            paras.add(t);
        }

        debug("KVS", kvs.toString());
        debug("BULLETS", bullets.toString());
        debug("PARAS", paras.toString());

        if (kvs.isEmpty() && bullets.isEmpty() && paras.isEmpty()) {
            return conservativeToHtml(normalized);
        }

        StringBuilder html = new StringBuilder(normalized.length() + 256);

        if (!kvs.isEmpty()) {
            html.append("<section class=\"desc-section table\"><h3>商品情報</h3><table>");
            for (KV kv : kvs) {
                html.append("<tr><th>").append(esc(kv.key)).append("</th><td>")
                        .append(esc(kv.value)).append("</td></tr>");
            }
            html.append("</table></section>");
        }
        if (!bullets.isEmpty()) {
            html.append("<section class=\"desc-section bullets\"><h3>特長・注意</h3><ul>");
            for (String b : bullets) {
                html.append("<li>").append(esc(b)).append("</li>");
            }
            html.append("</ul></section>");
        }
        if (!paras.isEmpty()) {
            html.append("<section class=\"desc-section body\">");
            for (String p : paras) {
                html.append("<p>").append(esc(p)).append("</p>");
            }
            html.append("</section>");
        }

        return clampToLastSection(html.toString());
    }

    // ---- Debug util ----
    private static void debug(String tag, String msg) {
        if (DEBUG) {
            System.out.println("[DEBUG] " + tag + " => " + msg);
        }
    }

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

    // ---- helpers ----

    private static String chooseBase(String html, String plain, String caption) {
        if (html != null && !html.isBlank()) return stripHtmlKeepBreaks(html);
        if (plain != null && !plain.isBlank()) return plain;
        return caption != null ? caption : "";
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String t = HtmlUtils.htmlUnescape(s);
        t = t.replace("\u00A0", " ");
        t = t.replace('\u3000', ' ');
        t = t.replaceAll("[\\t\\x0B\\f\\r]+", " ");
        return t.trim();
    }

    private static List<String> splitToLines(String text) {
        List<String> out = new ArrayList<>();
        String[] arr = text.replace("\r\n", "\n").replace("\r", "\n").split("\\n");
        for (String line : arr) {
            if (!line.trim().isEmpty()) out.add(line);
        }
        return out;
    }

    private static String conservativeToHtml(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = HtmlUtils.htmlUnescape(raw).replace("\r\n", "\n").replace("\r", "\n");
        String[] paras = s.split("\\n{2,}");
        StringBuilder html = new StringBuilder(s.length() + 128);
        for (String para : paras) {
            String body = para.strip().replaceAll("\\n", "<br>");
            if (!body.isEmpty()) {
                body = body
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;");
                html.append("<p>").append(body).append("</p>");
            }
        }
        return html.toString();
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
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private record KV(String key, String value) {}
}
