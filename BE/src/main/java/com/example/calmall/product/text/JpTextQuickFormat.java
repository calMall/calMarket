package com.example.calmall.product.text;

import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JpTextQuickFormat {

    private JpTextQuickFormat() {}

    // 文分割
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=。|！|!|？|\\?)\\s*");
    // 箇条書き
    private static final Pattern BULLET_HEAD = Pattern.compile("^\\s*(?:[・●\\-*•]|\\u2022)\\s*(.+)$");
    // KV
    private static final Pattern KV = Pattern.compile("^\\s*([^：:]{1,20})[：:]+\\s*(.+)$");

    // ノイズ
    private static final String[] NOISE = {
            "プレゼント・贈り物","御挨拶","お祝い","内祝い","御礼","謝礼",
            "よくある質問はこちら","FAQ","返品はできません","予めご了承ください"
    };

    public static String toReadablePlain(String raw) {
        if (!StringUtils.hasText(raw)) return "";

        // 正規化（HTMLエンティティ解除 + NFKC）
        String s = HtmlUtils.htmlUnescape(raw);
        s = Normalizer.normalize(s, Normalizer.Form.NFKC);
        s = s.replace("\u00A0", " ").replace('\u3000', ' ');
        s = s.replace("\r\n", "\n").replace("\r", "\n");
        s = s.replaceAll("[ \\t\\x0B\\f\\r]+", " ").trim();

        // 行・文へ
        List<String> lines = new ArrayList<>();
        for (String ln : s.split("\\n")) {
            String t = ln.trim();
            if (t.isEmpty()) continue;
            for (String v : SENTENCE_SPLIT.split(t)) {
                String st = v.trim();
                if (!st.isEmpty()) lines.add(st);
            }
        }

        // ノイズ・重複除去
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String t : lines) {
            if (isNoise(t)) continue;
            if (isOverlongWordList(t)) continue;
            out.add(t);
        }

        // KV → 箇条書き → 段落（プレーン表現）
        List<String> kvs = new ArrayList<>();
        List<String> bullets = new ArrayList<>();
        List<String> paras = new ArrayList<>();

        for (String t : out) {
            Matcher mk = KV.matcher(t);
            if (mk.find()) {
                String key = mk.group(1).trim();
                String val = mk.group(2).trim();
                if (!key.isEmpty() && !val.isEmpty() && !key.equals(val)) {
                    kvs.add(key + "： " + val);
                    continue;
                }
            }
            Matcher mb = BULLET_HEAD.matcher(t);
            if (mb.find()) {
                bullets.add(mb.group(1).trim());
                continue;
            }
            paras.add(t);
        }

        StringBuilder sb = new StringBuilder();
        if (!kvs.isEmpty()) {
            for (int i = 0; i < Math.min(12, kvs.size()); i++) {
                sb.append(kvs.get(i)).append("\n");
            }
        }
        if (!bullets.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            for (int i = 0; i < Math.min(12, bullets.size()); i++) {
                sb.append("・").append(bullets.get(i)).append("\n");
            }
        }
        if (!paras.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            for (int i = 0; i < Math.min(12, paras.size()); i++) {
                sb.append(paras.get(i)).append("\n");
            }
        }

        return sb.toString().replaceAll("\\n{3,}", "\n\n").trim();
    }

    // ---------- helpers ----------

    private static boolean isNoise(String t) {
        String s = t.replaceAll("\\s+", "");
        for (String w : NOISE) {
            if (s.contains(w.replaceAll("\\s+", ""))) return true;
        }
        return false;
    }

    private static boolean isOverlongWordList(String t) {
        int len = t.length();
        long marks = t.chars().filter(ch -> ch=='・' || ch=='、' || ch==',').count();
        return (len > 120 && marks > 5);
    }
}
