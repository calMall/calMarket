package com.example.calmall.product.text;

import com.example.calmall.ai.GroqClient;
import com.example.calmall.ai.GroqClient.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;


// LLMで商品説明の整形
@Slf4j
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


    // 原文（HTML / プレーン / キャプション）をLLMで整形
    public String cleanToHtml(String rawHtml, String rawPlain, String itemCaption) {
        final String base = chooseBasePreferHtml(rawHtml, rawPlain, itemCaption);
        if (!StringUtils.hasText(base)) {
            throw new IllegalStateException("No source text to clean.");
        }

        final String normalized = normalize(base);
        final List<String> chunks = chunkSmart(normalized, 1800);

        log.debug("[Groq LLM] chunks={}", chunks.size());

        final ExecutorService ex = Executors.newFixedThreadPool(Math.max(1, parallelism));
        final List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            final int idx = i;
            futures.add(ex.submit(() -> callGroqOnceWithRetry(idx, chunks.get(idx))));
        }
        ex.shutdown();

        final List<String> parts = new ArrayList<>();
        int ok = 0, ng = 0;
        for (Future<String> f : futures) {
            try {
                final String frag = f.get(60, TimeUnit.SECONDS);
                if (StringUtils.hasText(frag)) {
                    parts.add(frag);
                    ok++;
                } else {
                    ng++;
                }
            } catch (Exception e) {
                log.debug("[Groq LLM] chunk#{} failed (execution)", parts.size() + ng + 1, e);
                ng++;
            }
        }
        log.debug("[Groq LLM] finished: success={} failures={}", ok, ng);

        if (parts.isEmpty()) {
            throw new IllegalStateException("LLM produced no fragments (all chunks failed).");
        }

        final String merged = String.join("", parts);
        final String sanitized = sanitizeMerged(merged);
        assertNoLeadingBulletMarks(sanitized);
        return sanitized;
    }

    // Groq呼び出し

    private String callGroqOnceWithRetry(int chunkIndex, String chunk) throws IOException, InterruptedException {
        int attempt = 0;
        long backoff = 800;
        IOException last = null;

        while (attempt < 3) {
            try {
                return callGroq(chunkIndex, chunk);
            } catch (IOException e) {
                log.debug("[Groq LLM] chunk#{} attempt#{} failed: {}", chunkIndex + 1, attempt + 1, e.toString());
                last = e;
                Thread.sleep(backoff);
                backoff *= 2;
                attempt++;
            }
        }
        throw last != null ? last : new IOException("Groq call failed");
    }


    // system / user プロンプト
    private String callGroq(int chunkIndex, String chunk) throws IOException {
        final String system = """
あなたはECサイト向けの「商品説明テキストの構造化クリーナー」です。
入力は雑多・重複・順序崩れを含む可能性があります。

【出力フォーマット（厳守）】
1) 最初に1個だけ <style> を出力してもよい（省略可）。必ず `.product-desc` / `.desc-section` を先頭にした**スコープ付き**の軽量CSSのみ（概ね 500行/20KB 未満）。body/html/ユニバーサル（*）直指定、アニメーション、外部フォント、URL参照は禁止。
2) その直後に、埋め込み可能な**HTML断片**を出力する。先頭は必ず <section class="desc-section …"> から開始すること。

【許可される要素・属性（HTML側）】
<section class="desc-section table|bullets|body">、<table><tr><th|td>、<ul><li>、<p>
（<style> は先頭1個のみ許可。<script>・<link>・<iframe> などは禁止）

【CSSガイドライン】
- 影響範囲は `.product-desc` / `.desc-section` 配下のみに限定（例: `.product-desc .desc-section {...}`）。
- 表はボーダーと余白、箇条書きは行間・インデント、本文は段落間隔など最低限に留める。
- 配色はニュートラル（ダーク対応は prefers-color-scheme のみ）。

【変換ルール（本文）】
- 広告/クーポン/ショップ案内/FAQ/返品・交換/営業時間/連絡先/外部URL/JAN羅列/パンくず等のノイズは削除。
- 「規格/サイズ/容量/素材・成分/内容量/セット内容」などは可能な限り <table> の Key-Value に整理。
- 明確な列挙は <ul><li> に変換。それ以外は自然な文で <p> に要約（機能・特徴・使い方など）。
- 重複や同義反復は圧縮（「特徴特徴」→「特徴」など）。
- 架空の情報は作らない。外部リンクを生成しない。
- 出力は **CSS（任意）→HTML断片** のみ（<html> 等は不要）。
- 箇条書きの <li> テキスト先頭に『・』『●』『•』『-』『*』等の装飾記号を入れない。
- **注意書き・謝罪・指示文・プレースホルダー文（例：「入力が必要です。原文を入力してください。」）は出力しない。**
""";

        final String user = """
[チャンク #%d]
原文:
%s

出力要件:
- 先頭に1つだけ<style>（任意）→ 直後に<section>で始まるHTML断片
- 表にできない内容は<table>省略可
- 冗長や重複を整理して読みやすく
- 日本語の自然な文体
""".formatted(chunkIndex + 1, chunk);

        return groq.chat(
                model,
                List.of(Message.sys(system), Message.user(user)),
                maxTokens
        );
    }

    // HTMLを優先。無ければプレーン、最後にキャプション
    private static String chooseBasePreferHtml(String html, String plain, String caption) {
        if (StringUtils.hasText(html)) return html;
        if (StringUtils.hasText(plain)) return plain;
        return caption != null ? caption : "";
    }

    // 正規化
    private static String normalize(String s) {
        String t = (s == null) ? "" : HtmlUtils.htmlUnescape(s);
        t = t.replace("\r\n", "\n").replace("\r", "\n");
        return t.trim();
    }

    // 目安サイズで行単位にまとめる
    private static List<String> chunkSmart(String text, int targetLen) {
        final List<String> lines = Arrays.stream(text.split("\n"))
                .map(String::trim).filter(l -> !l.isEmpty()).toList();

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


    // LLM連結出力のサニタイズ
    private static String sanitizeMerged(String html) {
        if (html == null) return "";
        String s = html;

        s = s.replaceAll("(?is)<script[^>]*>.*?</script>", "");

        String keepStyle = "";
        java.util.regex.Matcher sm = java.util.regex.Pattern
                .compile("(?is)^\\s*<style[^>]*>(.*?)</style>")
                .matcher(s);
        if (sm.find()) {
            final String styleBlock = sm.group(0);
            final String styleBody  = sm.group(1);

            boolean hasGlobalSelector =
                    styleBody.matches("(?is).*(^|[\\s{;])(body|html|\\*)\\s*[,\\.{:#\\s].*");
            boolean hasForbiddenAtRules =
                    styleBody.matches("(?is).*(?:@keyframes|@font-face).*");
            boolean hasUrlUsage =
                    styleBody.matches("(?is).*url\\s*\\(.*\\).*");

            boolean scopedOk =
                    styleBody.matches("(?is).*(?:\\.(product-desc|desc-section)).*");

            if (!hasGlobalSelector && !hasForbiddenAtRules && !hasUrlUsage && scopedOk) {
                keepStyle = styleBlock;
            }
            s = s.substring(sm.end());
        }

        // 以降に現れる style は全部削除
        s = s.replaceAll("(?is)<style[^>]*>.*?</style>", "");

        // <section> より前のノイズを落とす
        s = s.replaceFirst("(?s)^\\s*[^<]*?(?=<section\\b)", "");

        // 最終的に <section> が先頭であること
        if (!s.trim().startsWith("<section")) {
            throw new IllegalStateException("LLM output did not start with <section> after sanitization.");
        }

        // よくある不要フレーズを削除
        String[] banPhrases = {
                "入力が必要です。原文を入力してください。",
                "please provide input",
                "no input provided",
                "placeholder",
                "これはテストです"
        };
        for (String bad : banPhrases) {
            s = s.replace(bad, "");
        }

        // 許容した style があれば先頭に戻す
        return (keepStyle + s).trim();
    }

    // <li> 先頭の装飾記号を禁止
    private static void assertNoLeadingBulletMarks(String html) {
        final var m = java.util.regex.Pattern
                .compile("<li>\\s*([・●•\\-*])", java.util.regex.Pattern.DOTALL)
                .matcher(html);
        if (m.find()) {
            throw new IllegalStateException(
                    "LLM bullet formatting violated: found leading bullet marks inside <li>.");
        }
    }
}
