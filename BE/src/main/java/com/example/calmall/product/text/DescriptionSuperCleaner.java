package com.example.calmall.product.text;

import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 楽天APIの説明文を「読みやすい安全なHTML断片」へ整形するユーティリティ。
 * - 外部ライブラリに依存せず <p>/<ul>/<li>/<table> のみ生成
 * - 規格/サイズ/素材・成分は <table> に寄せる
 * - ノイズ(JAN, 問合せ先, 装飾, 配送文言, 余計な見出し) を除去
 */
public final class DescriptionSuperCleaner {

    private DescriptionSuperCleaner() {}

    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern BULLET_HEAD = Pattern.compile("^[\\p{Z}\\t　]*[・●\\-*•◆■□◇▶▷◉◦]\\s*");
    private static final Pattern KV_PATTERN = Pattern.compile("^\\s*([^：:]{1,40})[：:]+\\s*(.+)$");
    private static final Pattern SPACES_MANY = Pattern.compile("[ \\t\\x0B\\f\\r　]+");

    private static final Set<String> JUNK_WORDS = setOf(
            "注意","特長","特長・注意","特徴","商品情報","商品説明","商品詳細","規格概要","仕様","スペック",
            "広告文責","販売元","製造元","メーカー","お問い合わせ","お問合せ","問合せ先","注意事項",
            "カテゴリ","カテゴリー","関連ワード","JANコード","型番","product code"
    );

    private static final Set<String> SPEC_KEYS = setOf("規格","サイズ","寸法","容量","枚数","適応体重","重量","入数","対象年齢");
    private static final Set<String> MATERIAL_KEYS = setOf("素材","成分","材質","表面材","吸収材","防水材","止着材","伸縮材","結合材");

    // =========================================================
    // Main API
    // =========================================================
    public static String buildCleanHtml(String descriptionHtml, String descriptionPlain, String itemCaption) {
        String base = chooseBase(descriptionHtml, descriptionPlain, itemCaption);
        String normalized = normalize(base);
        List<String> lines = Arrays.asList(normalized.split("\\n"));

        List<String[]> tableRows = new ArrayList<>();
        List<String> bullets = new ArrayList<>();
        List<String> paragraphs = new ArrayList<>();

        for (String ln : lines) {
            String t = ln.trim();
            if (t.isEmpty() || isJunkLine(t)) continue;

            // bullet
            if (BULLET_HEAD.matcher(t).find()) {
                bullets.add(t.replaceFirst(BULLET_HEAD.pattern(), "").trim());
                continue;
            }

            // key:value
            Matcher kv = KV_PATTERN.matcher(t);
            if (kv.find()) {
                tableRows.add(new String[]{kv.group(1).trim(), kv.group(2).trim()});
                continue;
            }

            // spec/material
            if (containsAny(t, SPEC_KEYS)) {
                tableRows.add(new String[]{"規格", t});
                continue;
            }
            if (containsAny(t, MATERIAL_KEYS)) {
                tableRows.add(new String[]{"素材・成分", t});
                continue;
            }

            paragraphs.add(t);
        }

        // HTML 出力
        StringBuilder out = new StringBuilder();

        if (!tableRows.isEmpty()) {
            out.append("<section class=\"desc-section table\"><table>");
            for (String[] row : tableRows) {
                out.append("<tr><th>").append(esc(row[0])).append("</th><td>")
                        .append(esc(row[1])).append("</td></tr>");
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

        return out.toString();
    }

    public static String toPlain(String html) {
        if (html == null) return "";
        String s = html.replaceAll("(?i)<\\s*br\\s*/?>", "\n")
                .replaceAll("(?i)</\\s*p\\s*>", "\n")
                .replaceAll("(?i)</\\s*li\\s*>", "\n")
                .replaceAll("(?i)</\\s*tr\\s*>", "\n");
        s = TAG_PATTERN.matcher(s).replaceAll("");
        s = HtmlUtils.htmlUnescape(s);
        s = SPACES_MANY.matcher(s).replaceAll(" ");
        return s.replaceAll("\\n{2,}", "\n").trim();
    }

    private static String chooseBase(String html, String plain, String caption) {
        if (html != null && !html.isBlank()) return stripHtmlKeepBreaks(html);
        if (plain != null && !plain.isBlank()) return plain;
        return caption != null ? caption : "";
    }

    private static String stripHtmlKeepBreaks(String html) {
        String s = html.replaceAll("(?i)<\\s*br\\s*/?>", "\n")
                .replaceAll("(?i)</\\s*p\\s*>", "\n")
                .replaceAll("(?i)</\\s*li\\s*>", "\n")
                .replaceAll("(?i)</\\s*tr\\s*>", "\n");
        return TAG_PATTERN.matcher(s).replaceAll("");
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String t = HtmlUtils.htmlUnescape(s);
        t = t.replace('\u00A0', ' ').replace('\u3000', ' ');
        t = t.replace("\r\n", "\n").replace("\r", "\n");
        return SPACES_MANY.matcher(t).replaceAll(" ").trim();
    }

    private static boolean isJunkLine(String t) {
        String s = t.trim();
        if (JUNK_WORDS.contains(s)) return true;
        if (s.length() <= 2) return true;
        return false;
    }

    private static boolean containsAny(String s, Set<String> dict) {
        for (String k : dict) if (s.contains(k)) return true;
        return false;
    }

    private static String esc(String s) {
        return HtmlUtils.htmlEscape(s == null ? "" : s);
    }

    private static Set<String> setOf(String... arr) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(arr)));
    }
}
