package com.example.calmall.product.text;

import com.example.calmall.ai.GroqClient;
import com.example.calmall.ai.GroqClient.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * LLM を用いた商品説明の構造化整形。
 *
 * 【重要変更点】
 * - 例外の握りつぶしを廃止。チャンク単位で失敗をカウントし、全滅なら例外を投げる（fail-fast）。
 * - すべて失敗（= parts.isEmpty()）時の「ローカル整形フォールバック」を撤廃（Facade側の方針に合わせる）。
 * - 各ステップで DEBUG ログを出力（チャンク数/成功数/失敗数）。
 */
public class LlmDescriptionFormatter {

    private static final Logger log = LoggerFactory.getLogger(LlmDescriptionFormatter.class);

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
        // 事前チェック：client/model
        if (groq == null) {
            throw new IllegalStateException("Groq client is null (not configured).");
        }
        if (!StringUtils.hasText(model)) {
            throw new IllegalStateException("Groq model is empty.");
        }

        // 入力基準選択（HTML優先）
        final String base = DescriptionSuperCleanerBase.chooseBasePreferHtml(rawHtml, rawPlain, itemCaption);
        if (!StringUtils.hasText(base)) {
            throw new IllegalStateException("No description source text (html/plain/caption are all blank).");
        }

        // 正規化 → チャンク化
        final String normalized = DescriptionSuperCleanerBase.normalize(base);
        final List<String> chunks = chunkSmart(normalized, 1800);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("Chunking produced no input.");
        }
        log.debug("[Groq LLM] chunks={}", chunks.size());

        // 並列実行
        final ExecutorService ex = Executors.newFixedThreadPool(Math.max(1, parallelism));
        final List<Future<String>> futures = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            final int idx = i;
            futures.add(ex.submit(() -> callGroqOnceWithRetry(idx, chunks.get(idx))));
        }
        ex.shutdown();

        // 回収（失敗は握りつぶさずカウント）
        final List<String> parts = new ArrayList<>();
        int failures = 0;

        for (int i = 0; i < futures.size(); i++) {
            final Future<String> f = futures.get(i);
            try {
                final String frag = f.get(45, TimeUnit.SECONDS);
                if (StringUtils.hasText(frag)) {
                    parts.add(frag.trim());
                } else {
                    failures++;
                    log.debug("[Groq LLM] chunk#{} returned blank", i + 1);
                }
            } catch (TimeoutException te) {
                failures++;
                log.debug("[Groq LLM] chunk#{} timeout", i + 1, te);
            } catch (ExecutionException ee) {
                failures++;
                log.debug("[Groq LLM] chunk#{} failed (execution)", i + 1, ee.getCause());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                failures++;
                log.debug("[Groq LLM] chunk#{} interrupted", i + 1, ie);
            }
        }

        log.debug("[Groq LLM] finished: success={} failures={}", parts.size(), failures);

        if (parts.isEmpty()) {
            // ★ここが最大の変更点：内部フォールバックは行わず、明示的に失敗させる
            throw new IllegalStateException("LLM produced no fragments (all chunks failed).");
        }

        // 断片の統合 → 安全クランプ
        final String merged = HtmlMerge.mergeFragments(parts);
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
                log.debug("[Groq LLM] chunk#{} attempt#{} failed: {}", chunkIndex + 1, attempt + 1, e.toString());
                Thread.sleep(backoff);
                backoff *= 2;
                attempt++;
            }
        }
        throw (last != null ? last : new IOException("Groq call failed"));
    }

    /** 日本語プロンプト（HTML断片のみを返す・許可タグ制約を明示） */
    private String callGroq(int chunkIndex, String chunk) throws IOException {
        final String system = """
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

        final String user = """
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
        final List<String> lines = Arrays.stream(text.replace("\r","").split("\n"))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();

        final List<String> out = new ArrayList<>();
        final StringBuilder buf = new StringBuilder(targetLen + 256);
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

            final StringBuilder out = new StringBuilder(2048);
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
            final int i = html.lastIndexOf("</section>");
            return (i >= 0) ? html.substring(0, i + 10) : html;
        }

        // --- 抽出・重複除去 ---
        private static List<String[]> extractRows(String html) {
            final List<String[]> list = new ArrayList<>();
            final var m = java.util.regex.Pattern
                    .compile("<tr>\\s*<th>(.*?)</th>\\s*<td>(.*?)</td>\\s*</tr>", java.util.regex.Pattern.DOTALL)
                    .matcher(html);
            while (m.find()) {
                final String k = stripTags(m.group(1)).trim();
                final String v = stripTags(m.group(2)).trim();
                if (!k.isEmpty() && !v.isEmpty()) list.add(new String[]{k, v});
            }
            return list;
        }

        private static List<String> extractLis(String html) {
            final List<String> list = new ArrayList<>();
            final var m = java.util.regex.Pattern
                    .compile("<li>(.*?)</li>", java.util.regex.Pattern.DOTALL)
                    .matcher(html);
            while (m.find()) {
                final String v = stripTags(m.group(1)).trim();
                if (!v.isEmpty()) list.add(v);
            }
            return list;
        }

        private static List<String> extractPs(String html) {
            final List<String> list = new ArrayList<>();
            final var m = java.util.regex.Pattern
                    .compile("<p>(.*?)</p>", java.util.regex.Pattern.DOTALL)
                    .matcher(html);
            while (m.find()) {
                final String v = stripTags(m.group(1)).trim();
                if (!v.isEmpty()) list.add(v);
            }
            return list;
        }

        private static List<String[]> dedupeRows(List<String[]> rows) {
            final Map<String, String> map = new LinkedHashMap<>();
            for (String[] r : rows) {
                final String k = normalize(r[0]);
                final String v = normalize(r[1]);
                if (!map.containsKey(k)) map.put(k, v);
                else if (map.get(k).length() < v.length()) map.put(k, v); // 情報量が多い方を採用
            }
            return map.entrySet().stream().map(e -> new String[]{e.getKey(), e.getValue()})
                    .collect(Collectors.toList());
        }

        private static List<String> dedupeList(List<String> list) {
            final LinkedHashSet<String> set = new LinkedHashSet<>();
            for (String s : list) set.add(normalize(s));
            return new ArrayList<>(set);
        }

        private static String stripTags(String s) {
            return s.replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("<[^>]+>", "");
        }
        private static String normalize(String s) {
            String t = s.replace('\u00A0',' ')
                    .replaceAll("[ \\t\\x0B\\f\\r　]+"," ")
                    .trim();
            t = t.replaceAll("^(特徴)\\1+", "$1"); // 「特徴特徴」→「特徴」
            return t;
        }
        private static String esc(String s) {
            final StringBuilder sb = new StringBuilder(s.length()+16);
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
