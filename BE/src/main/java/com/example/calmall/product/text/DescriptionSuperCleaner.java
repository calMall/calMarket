//package com.example.calmall.product.text;
//
//import org.springframework.web.util.HtmlUtils;
//
//import java.util.*;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public final class DescriptionSuperCleaner {
//
//    private DescriptionSuperCleaner() {}
//
//    // =========================================================
//    // 正規表現
//    // =========================================================
//    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
//    private static final Pattern BULLET_HEAD = Pattern.compile("^[\\p{Z}\\t　]*[・●\\-*•◆■□◇▶▷◉◦]\\s*");
//    private static final Pattern KV_PATTERN = Pattern.compile("^\\s*([^：:]{1,40})[：:]+\\s*(.+)$");
//    private static final Pattern SPACES_MANY = Pattern.compile("[ \\t\\x0B\\f\\r　]+");
//    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=。|！|!|？|\\?|\\.|、)(?!$)");
//
//    // ノイズ除去キーワード
//    private static final Set<String> NOISE_WORDS = Set.of(
//            "特長", "特長・注意", "注意事項", "商品情報", "商品詳細", "※", "【", "】"
//    );
//
//    // =========================================================
//    // 公開API
//    // =========================================================
//    public static String buildCleanHtml(String descriptionHtml, String descriptionPlain, String itemCaption) {
//        String base = chooseBase(descriptionHtml, descriptionPlain, itemCaption);
//        String normalized = normalize(base);
//
//        // 行分割 + 長文 fallback
//        List<String> lines = splitToLinesWithFallback(normalized);
//        lines = mergeBrokenLines(lines);
//
//        List<String> specLines = new ArrayList<>();
//        List<String> materialLines = new ArrayList<>();
//        List<String> featureLines = new ArrayList<>();
//
//        for (String ln : lines) {
//            String t = ln.trim();
//            if (t.isEmpty() || isNoise(t)) continue;
//
//            // カテゴリ分け
//            if (t.contains("規格") || t.contains("サイズ")) {
//                specLines.add(t);
//                continue;
//            }
//            if (t.contains("素材") || t.contains("成分")) {
//                materialLines.add(t);
//                continue;
//            }
//            featureLines.add(t);
//        }
//
//        // HTML 構築
//        StringBuilder out = new StringBuilder();
//
//        if (!specLines.isEmpty()) {
//            out.append("<section class=\"desc-section\"><h4>仕様</h4><ul>");
//            for (String s : specLines) {
//                out.append("<li>").append(esc(cleanNoise(s))).append("</li>");
//            }
//            out.append("</ul></section>");
//        }
//
//        if (!materialLines.isEmpty()) {
//            out.append("<section class=\"desc-section\"><h4>素材・成分</h4><ul>");
//            for (String m : materialLines) {
//                out.append("<li>").append(esc(cleanNoise(m))).append("</li>");
//            }
//            out.append("</ul></section>");
//        }
//
//        if (!featureLines.isEmpty()) {
//            out.append("<section class=\"desc-section\"><h4>特徴</h4><ul>");
//            for (String f : featureLines) {
//                out.append("<li>").append(esc(cleanNoise(f))).append("</li>");
//            }
//            out.append("</ul></section>");
//        }
//
//        return clampToLastSection(out.toString());
//    }
//
//    /**
//     * HTML からタグを除去してプレーンテキスト化
//     */
//    public static String toPlain(String html) {
//        if (html == null) return "";
//        String s = html;
//        s = s.replaceAll("(?i)<\\s*br\\s*/?>", "\n");
//        s = s.replaceAll("(?i)</\\s*p\\s*>", "\n");
//        s = s.replaceAll("(?i)</\\s*li\\s*>", "\n");
//        s = s.replaceAll("(?i)</\\s*tr\\s*>", "\n");
//        s = TAG_PATTERN.matcher(s).replaceAll("");
//        s = HtmlUtils.htmlUnescape(s);
//        s = s.replace("\u00A0", " ");
//        s = SPACES_MANY.matcher(s).replaceAll(" ");
//        s = s.replaceAll("\\n{2,}", "\n").trim();
//        return s;
//    }
//
//    // =========================================================
//    // 内部ユーティリティ
//    // =========================================================
//    private static String chooseBase(String html, String plain, String caption) {
//        if (html != null && !html.isBlank()) return stripHtmlKeepBreaks(html);
//        if (plain != null && !plain.isBlank()) return plain;
//        return caption != null ? caption : "";
//    }
//
//    private static String stripHtmlKeepBreaks(String html) {
//        String s = html;
//        s = s.replaceAll("(?i)<\\s*br\\s*/?>", "\n");
//        s = s.replaceAll("(?i)</\\s*p\\s*>", "\n");
//        s = s.replaceAll("(?i)</\\s*li\\s*>", "\n");
//        s = s.replaceAll("(?i)</\\s*tr\\s*>", "\n");
//        s = TAG_PATTERN.matcher(s).replaceAll("");
//        return s;
//    }
//
//    private static String normalize(String s) {
//        if (s == null) return "";
//        String t = HtmlUtils.htmlUnescape(s);
//        t = t.replace('\u00A0', ' ').replace('\u3000', ' ');
//        t = t.replace("\r\n", "\n").replace("\r", "\n");
//        return t.trim();
//    }
//
//    private static List<String> splitToLinesWithFallback(String text) {
//        List<String> lines = new ArrayList<>();
//        String[] raw = text.split("\\n");
//        for (String r : raw) {
//            String t = r.trim();
//            if (t.isEmpty()) continue;
//            if (t.length() > 120 && t.indexOf('。') >= 0) {
//                for (String s : SENTENCE_SPLIT.split(t)) {
//                    String x = s.trim();
//                    if (!x.isEmpty()) lines.add(x);
//                }
//            } else {
//                lines.add(t);
//            }
//        }
//        return lines;
//    }
//
//    private static List<String> mergeBrokenLines(List<String> lines) {
//        List<String> merged = new ArrayList<>();
//        StringBuilder buf = new StringBuilder();
//        for (String l : lines) {
//            if (!l.endsWith("。") && !l.endsWith("」") && !l.endsWith("』") && l.length() < 40) {
//                buf.append(l);
//            } else {
//                if (buf.length() > 0) {
//                    buf.append(l);
//                    merged.add(buf.toString());
//                    buf.setLength(0);
//                } else {
//                    merged.add(l);
//                }
//            }
//        }
//        if (buf.length() > 0) merged.add(buf.toString());
//        return merged;
//    }
//
//    private static boolean isNoise(String s) {
//        for (String n : NOISE_WORDS) {
//            if (s.contains(n)) return true;
//        }
//        return false;
//    }
//
//    private static String cleanNoise(String s) {
//        String t = s;
//        for (String n : NOISE_WORDS) {
//            t = t.replace(n, "");
//        }
//        return t.trim();
//    }
//
//    private static String clampToLastSection(String html) {
//        if (html == null) return null;
//        int i = html.lastIndexOf("</section>");
//        return (i >= 0) ? html.substring(0, i + "</section>".length()) : html;
//    }
//
//    private static String esc(String s) {
//        if (s == null) return "";
//        StringBuilder sb = new StringBuilder(s.length() + 16);
//        for (char c : s.toCharArray()) {
//            switch (c) {
//                case '<' -> sb.append("&lt;");
//                case '>' -> sb.append("&gt;");
//                case '&' -> sb.append("&amp;");
//                case '"' -> sb.append("&quot;");
//                case '\'' -> sb.append("&#39;");
//                default -> sb.append(c);
//            }
//        }
//        return sb.toString();
//    }
//}
