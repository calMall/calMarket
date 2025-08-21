package com.example.calmall.product.text;

import com.example.calmall.ai.GroqClient;
import com.example.calmall.ai.GroqClient.Message;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * LLM を用いた商品説明の構造化整形。
 * - 長文対応：チャンク分割＋控えめ並列
 * - 断片HTMLの統合と重複除去
 * - 失敗時はローカル整形にフォールバック
 */
public class LlmDescriptionFormatter {

    private final GroqClient groq;
    private final String model;
    private final int maxTokens;
    private final int parallelism;

    public LlmDescriptionFormatter(GroqClient groq, String model, int maxTokens, int parallelism) {
        this.groq = groq;
        this.model = model;
        this.maxTokens = maxTokens;
        this.parallelism = parallelism;
    }

    /** 原文（HTML/プレーン/キャプション）→ LLM で構造化 HTML */
    public String cleanToHtml(String rawHtml, String rawPlain, String itemCaption) {
        String base = DescriptionSuperCleanerBase.chooseBasePreferHtml(rawHtml, rawPlain, itemCaption);
        if (!StringUtils.hasText(base)) return "";

        String normalized = DescriptionSuperCleanerBase.normalize(base);
        List<String> chunks = chunkSmart(normalized, 1800);

        ExecutorService ex = Executors.newFixedThreadPool(Math.max(1, parallelism));
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            final int idx = i;
            futures.add(ex.submit(() -> callGroqOnceWithRetry(idx, chunks.get(idx))));
        }
        ex.shutdown();

        List<String> parts = new ArrayList<>();
        for (Future<String> f : futures) {
            try {
                String frag = f.get(45, TimeUnit.SECONDS);
                if (StringUtils.hasText(frag)) parts.add(frag.trim());
            } catch (Exception ignored) {}
        }

        if (parts.isEmpty()) {
            // すべて失敗した場合はローカル整形で最低限を担保
            return DescriptionSuperCleanerBase.localCleanToHtml(rawHtml, rawPlain, itemCaption);
        }

        String merged = HtmlMerge.mergeFragments(parts);
        return HtmlMerge.safeClamp(merged);
    }

    // --- Groq 呼び出し（指数バックオフ付） ---
    private String callGroqOnceWithRetry(int chunkIndex, String chunk) throws IOException, InterruptedException {
        int attempt = 0;
        long backoff = 800;
        IOException last = null;

        while (attempt < 3) {
            try {
                return callGroq(chunkIndex, chunk);
            } catch (IOException e) {
                last = e;
                Thread.sleep(backoff);
                backoff *= 2;
                attempt++;
            }
        }
        throw last != null ? last : new IOException("Groq call failed");
    }

    /** ★日本語プロンプト（HTML断片のみを返す・許可タグ制約を明示） */
    private String callGroq(int chunkIndex, String chunk) throws IOException {
        String system = """
あなたはECサイト向けの「商品説明テキストの構造化クリーナー」です。
入力は雑多・重複・順序崩れを含む可能性があります。
以下の制約に従い、埋め込み可能な安全な HTML 断片だけを出力してください。

【許可される要素・属性（厳守）】
<section class="desc-section table|bullets|body">、<table><tr><th|td>、<ul><li>、<p>

【変換ルール】
- 広告/クーポン/ショップ案内/FAQ/返品・交換/営業時間/連絡先/外部URL/JAN羅列/パンくず等のノイズは削除。
- 「規格/サイズ/容量/素材・成分/内容量/セット内容」などは可能な限り <table> の Key-Value に整理。
- 明確な列挙は <ul><li> に変換。
- それ以外は自然な文で <p> として要約（機能・特徴・効果・使い方など）。
- 重複や同義反復は圧縮（例：「特徴特徴」→「特徴」）。
- 架空の情報を作らない。外部リンクを生成しない。
- 片段（フラグメント）のみ返す（<html> 等は不要）。
""";

        String user = """
[チャンク #%d]
原文:
%s

出力要件:
- 出力は HTML 断片のみ（<section> + <table>/<ul>/<p>）
- 表にできない場合は <table> を省略可
- 冗長/重複語の圧縮・言い換えで可読性を高める
- 日本語の自然な文体で整形
""".formatted(chunkIndex + 1, chunk);

        return groq.chat(
                model,
                List.of(Message.sys(system), Message.user(user)),
                maxTokens
        );
    }

    // --- テキスト分割（行ベース＋サイズ目安） ---
    private static List<String> chunkSmart(String text, int targetLen) {
        List<String> lines = Arrays.stream(text.replace("\r","").split("\n"))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();

        List<String> out = new ArrayList<>();
        StringBuilder buf = new StringBuilder(targetLen + 256);
        for (String ln : lines) {
            if (buf.length() + ln.length() + 1 > targetLen) {
                out.add(buf.toString().trim());
                buf.setLength(0);
            }
            if (buf.length() > 0) buf.append('\n');
            buf.append(ln);
        }
        if (buf.length() > 0) out.add(buf.toString().trim());
        return out;
    }

    /** 断片 HTML をマージ：表/箇条書き/本文の抽出→重複除去→結合 */
    public static class HtmlMerge {
        public static String mergeFragments(List<String> fragments) {
            List<String[]> rows = new ArrayList<>();
            List<String> lis = new ArrayList<>();
            List<String> ps  = new ArrayList<>();

            for (String frag : fragments) {
                rows.addAll(extractRows(frag));
                lis.addAll(extractLis(frag));
                ps.addAll(extractPs(frag));
            }

            rows = dedupeRows(rows);
            lis  = dedupeList(lis);
            ps   = dedupeList(ps);

            StringBuilder out = new StringBuilder(2048);
            if (!rows.isEmpty()) {
                out.append("<section class=\"desc-section table\"><table>");
                for (String[] r : rows) {
                    out.append("<tr><th>").append(esc(r[0])).append("</th><td>").append(esc(r[1])).append("</td></tr>");
                }
                out.append("</table></section>");
            }
            if (!lis.isEmpty()) {
                out.append("<section class=\"desc-section bullets\"><ul>");
                for (String li : lis) out.append("<li>").append(esc(li)).append("</li>");
                out.append("</ul></section>");
            }
            if (!ps.isEmpty()) {
                out.append("<section class=\"desc-section body\">");
                for (String p : ps) out.append("<p>").append(esc(p)).append("</p>");
                out.append("</section>");
            }
            return out.toString();
        }

        public static String safeClamp(String html) {
            int i = html.lastIndexOf("</section>");
            return (i >= 0) ? html.substring(0, i + 10) : html;
        }

        // --- 抽出・重複除去 ---
        private static List<String[]> extractRows(String html) {
            List<String[]> list = new ArrayList<>();
            var m = java.util.regex.Pattern
                    .compile("<tr>\\s*<th>(.*?)</th>\\s*<td>(.*?)</td>\\s*</tr>", java.util.regex.Pattern.DOTALL)
                    .matcher(html);
            while (m.find()) {
                String k = stripTags(m.group(1)).trim();
                String v = stripTags(m.group(2)).trim();
                if (!k.isEmpty() && !v.isEmpty()) list.add(new String[]{k, v});
            }
            return list;
        }

        private static List<String> extractLis(String html) {
            List<String> list = new ArrayList<>();
            var m = java.util.regex.Pattern
                    .compile("<li>(.*?)</li>", java.util.regex.Pattern.DOTALL)
                    .matcher(html);
            while (m.find()) {
                String v = stripTags(m.group(1)).trim();
                if (!v.isEmpty()) list.add(v);
            }
            return list;
        }

        private static List<String> extractPs(String html) {
            List<String> list = new ArrayList<>();
            var m = java.util.regex.Pattern
                    .compile("<p>(.*?)</p>", java.util.regex.Pattern.DOTALL)
                    .matcher(html);
            while (m.find()) {
                String v = stripTags(m.group(1)).trim();
                if (!v.isEmpty()) list.add(v);
            }
            return list;
        }

        private static List<String[]> dedupeRows(List<String[]> rows) {
            Map<String, String> map = new LinkedHashMap<>();
            for (String[] r : rows) {
                String k = normalize(r[0]);
                String v = normalize(r[1]);
                if (!map.containsKey(k)) map.put(k, v);
                else if (map.get(k).length() < v.length()) map.put(k, v); // 情報量が多い方を採用
            }
            return map.entrySet().stream().map(e -> new String[]{e.getKey(), e.getValue()}).collect(Collectors.toList());
        }

        private static List<String> dedupeList(List<String> list) {
            LinkedHashSet<String> set = new LinkedHashSet<>();
            for (String s : list) set.add(normalize(s));
            return new ArrayList<>(set);
        }

        private static String stripTags(String s) {
            return s.replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("<[^>]+>", "");
        }
        private static String normalize(String s) {
            String t = s.replace('\u00A0',' ').replaceAll("[ \\t\\x0B\\f\\r　]+"," ").trim();
            t = t.replaceAll("^(特徴)\\1+", "$1"); // 「特徴特徴」→「特徴」
            return t;
        }
        private static String esc(String s) {
            StringBuilder sb = new StringBuilder(s.length()+16);
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '<' -> sb.append("&lt;");
                    case '>' -> sb.append("&gt;");
                    case '&' -> sb.append("&amp;");
                    case '"' -> sb.append("&quot;");
                    case '\''-> sb.append("&#39;");
                    default  -> sb.append(c);
                }
            }
            return sb.toString();
        }
    }
}
