package com.example.calmall.product.text;

import org.springframework.web.util.HtmlUtils;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DescriptionSuperCleaner {

    private static final Pattern KEY_VALUE = Pattern.compile("^\\s*([^：:]{1,20})[：:][\\s　]*(.+)$");
    private static final Pattern BULLET = Pattern.compile("^[\\s　]*([・\\-\\*●■◆\\-]|【.*】).*");

    private DescriptionSuperCleaner() {}

    public static String buildCleanHtml(String descriptionHtml, String descriptionPlain, String itemCaption) {
        String base = selectBase(descriptionPlain, descriptionHtml, itemCaption);
        if (base.isBlank()) return "";

        // 標準化
        String normalized = normalize(base);

        // 行ごと
        List<String> rawLines = Arrays.asList(normalized.split("\\n+"));
        List<String> lines = mergeBrokenLines(rawLines);

        // 分類
        List<String[]> table = new ArrayList<>();
        List<String> bullets = new ArrayList<>();
        List<String> paras = new ArrayList<>();

        for (String ln : lines) {
            String t = squeeze(ln);
            if (t.isEmpty()) continue;
            if (t.startsWith("特長・注意") || t.equals("注意")) continue;

            Matcher kv = KEY_VALUE.matcher(t);
            if (kv.find()) {
                table.add(new String[]{kv.group(1).trim(), kv.group(2).trim()});
                continue;
            }
            if (BULLET.matcher(t).matches() || t.startsWith("※") || t.startsWith("【")) {
                bullets.add(t.replaceFirst("^[\\s　・\\-\\*●■◆【】()（）]+", "").trim());
                continue;
            }
            paras.add(t);
        }

        // HTML 組立
        StringBuilder out = new StringBuilder(1024);

        if (!table.isEmpty()) {
            out.append("<section class=\"desc-section table\"><h3>商品情報</h3><table>");
            for (String[] row : table) {
                out.append("<tr><th>").append(esc(row[0]))
                        .append("</th><td>").append(esc(row[1])).append("</td></tr>");
            }
            out.append("</table></section>");
        }

        if (!bullets.isEmpty()) {
            out.append("<section class=\"desc-section bullets\"><h3>注意事項</h3><ul>");
            for (String b : bullets) {
                out.append("<li>").append(esc(b)).append("</li>");
            }
            out.append("</ul></section>");
        }

        if (!paras.isEmpty()) {
            out.append("<section class=\"desc-section body\">");
            for (String p : paras) {
                out.append("<p>").append(esc(p)).append("</p>");
            }
            out.append("</section>");
        }

        return out.toString();
    }

    // ---------------- utils ----------------

    private static String esc(String s) {
        return HtmlUtils.htmlEscape(s == null ? "" : s);
    }

    private static String selectBase(String plain, String html, String cap) {
        if (plain != null && !plain.isBlank()) return plain;
        if (html != null && !html.isBlank()) return html.replaceAll("<[^>]+>", "\n");
        return cap == null ? "" : cap.replaceAll("<[^>]+>", "\n");
    }

    private static String normalize(String s) {
        return s.replace("&nbsp;", " ")
                .replace("&times;", "×")
                .replaceAll("[\\r\\t]+", "")
                .replaceAll("　", " ")
                .trim();
    }

    private static String squeeze(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    /** 連結された文をマージ（句読点が無い行を次行にくっつける） */
    private static List<String> mergeBrokenLines(List<String> raw) {
        List<String> out = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (String ln : raw) {
            String t = squeeze(ln);
            if (t.isEmpty()) continue;

            if (buf.length() > 0) {
                buf.append(t);
                out.add(buf.toString());
                buf.setLength(0);
            } else {
                if (!t.endsWith("。") && !t.endsWith("！") && !t.endsWith("】")) {
                    buf.append(t);
                } else {
                    out.add(t);
                }
            }
        }
        if (buf.length() > 0) out.add(buf.toString());
        return out;
    }
}
