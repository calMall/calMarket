package com.example.calmall.product.text;

import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public final class DescriptionSuperCleaner {

    private DescriptionSuperCleaner() {}

    // 文の分割（句点/疑問符/感嘆符）
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=。|！|!|？|\\?)\\s*");

    // キー：値（キーは20文字以内）
    private static final Pattern KV_PATTERN = Pattern.compile("^\\s*([^：:]{1,20})[：:]+\\s*(.+)$");

    // 箇条書き
    private static final Pattern BULLET_PATTERN = Pattern.compile("^\\s*(?:[・●\\-*•]|\\u2022)\\s*(.+)$");

    // タグ
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");

    // ノイズ語彙（空白無視で含有判定）
    private static final String[] NOISE_PHRASES = {
            "プレゼント・贈り物","御挨拶","お祝い","内祝い","御礼","謝礼",
            "母の日","父の日","お歳暮","御歳暮","クリスマス","バレンタイン","ホワイトデー","敬老の日",
            "季節の贈り物","引越し","就職祝い","卒業","入学","ブライダル","記念品",
            "ギフト・プチギフト","こんなお相手に","こんなメッセージに",
            "よくある質問はこちら","FAQ","返品はできません","予めご了承ください"
    };

    // ---------- public ----------

    // HTML を構築（最小安全タグ）
    public static String buildCleanHtml(String descriptionHtml, String descriptionPlain, String itemCaption) {
        // 1) ベーステキスト決定（HTMLがあれば一度プレーン化）
        String base = chooseBase(descriptionHtml, descriptionPlain, itemCaption);

        // 2) 正規化
        String normalized = normalize(base);

        // 3) 文/行へ分割
        List<String> lines = splitToLines(normalized);

        // 4) ノイズ・重複除外
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String ln : lines) {
            String t = ln.trim();
            if (t.isEmpty()) continue;
            if (isNoise(t)) continue;
            if (isOverlongWordList(t)) continue;
            set.add(t);
        }
        List<String> clean = new ArrayList<>(set);

        // 5) 構造化（KV/UL/P）
        List<KV> kvs = new ArrayList<>();
        List<String> bullets = new ArrayList<>();
        List<String> paras = new ArrayList<>();

        for (String t : clean) {
            Matcher mkv = KV_PATTERN.matcher(t);
            if (mkv.find()) {
                String key = mkv.group(1).trim();
                String val = mkv.group(2).trim();
                // th=td の重複は除外
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

        // 6) HTML組み立て（各12件までに制限）
        StringBuilder html = new StringBuilder();
        if (!kvs.isEmpty()) {
            html.append("<section class=\"desc-section table\"><h3>商品情報</h3><table>");
            for (int i = 0; i < Math.min(12, kvs.size()); i++) {
                KV kv = kvs.get(i);
                html.append("<tr><th>").append(esc(kv.key)).append("</th><td>")
                        .append(esc(kv.value)).append("</td></tr>");
            }
            html.append("</table></section>");
        }
        if (!bullets.isEmpty()) {
            html.append("<section class=\"desc-section bullets\"><h3>特長・注意</h3><ul>");
            for (int i = 0; i < Math.min(12, bullets.size()); i++) {
                html.append("<li>").append(esc(bullets.get(i))).append("</li>");
            }
            html.append("</ul></section>");
        }
        if (!paras.isEmpty()) {
            html.append("<section class=\"desc-section body\">");
            for (int i = 0; i < Math.min(12, paras.size()); i++) {
                html.append("<p>").append(esc(paras.get(i))).append("</p>");
            }
            html.append("</section>");
        }

        // 7) 末尾に混入したプレーンを切り落とす保険
        return clampToLastSection(html.toString());
    }

    // HTML → 読みやすいプレーンへ
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

    // ---------- internal ----------

    private static String chooseBase(String html, String plain, String caption) {
        if (html != null && html.contains("<")) return stripHtmlKeepBreaks(html);
        if (plain != null && !plain.isBlank())  return plain;
        return caption != null ? caption : "";
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String t = HtmlUtils.htmlUnescape(s);
        t = t.replace("\u00A0", " ");                 // NBSP
        t = t.replace('\u3000', ' ');                 // 全角スペース
        t = t.replaceAll("[ \\t\\x0B\\f\\r]+", " ");  // 連続空白圧縮
        return t.trim();
    }

    private static List<String> splitToLines(String text) {
        List<String> out = new ArrayList<>();
        for (String line : text.split("\\n")) {
            String ln = line.trim();
            if (ln.isEmpty()) continue;
            for (String s : SENTENCE_SPLIT.split(ln)) {
                String st = s.trim();
                if (!st.isEmpty()) out.add(st);
            }
        }
        return out;
    }

    private static boolean isNoise(String t) {
        String s = t.replaceAll("\\s+", "");
        for (String w : NOISE_PHRASES) {
            if (s.contains(w.replaceAll("\\s+", ""))) return true;
        }
        // お問い合わせ・長文テンプレ等
        if (s.matches(".*(よくある質問|FAQ|返品|変更|できません).*")) return true;
        return false;
    }

    private static boolean isOverlongWordList(String t) {
        int len = t.length();
        long marks = t.chars().filter(ch -> ch=='・' || ch=='、' || ch==',').count();
        return (len > 120 && marks > 5);
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
