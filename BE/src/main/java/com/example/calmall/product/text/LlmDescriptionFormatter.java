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

/**
 * LLMを用いた説明文整形
 * ・入力テキストの分割
 * ・LLM呼び出し（控えめ並列）
 * ・出力の軽い検証とサニタイズ
 */
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

    /**
     * 原文（HTML/プレーン/キャプション）をLLMで整形してHTML断片を返す
     */
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

        // LLM出力を連結 → サニタイズ → 軽い検証
        final String merged = String.join("", parts);
        final String sanitized = sanitizeMerged(merged);
        assertNoLeadingBulletMarks(sanitized);

        return sanitized;
    }

    // ----- Groq 呼び出し（指数バックオフ） -----

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

    /**
     * LLMプロンプト
     * ・先頭に余計な文言を出さない
     * ・<li>内に装飾記号を書かない
     */
    private String callGroq(int chunkIndex, String chunk) throws IOException {
        final String system = """
あなたはECサイト向けの「商品説明テキストの構造化クリーナー」です。
入力は雑多・重複・順序崩れを含む可能性があります。
以下の制約に従い、埋め込み可能な安全な HTML 断片だけを出力してください。

【許可される要素・属性（厳守）】
<section class="desc-section table|bullets|body">、<table><tr><th|td>、<ul><li>、<p>

【変換ルール】
- 広告/クーポン/ショップ案内/FAQ/返品・交換/営業時間/連絡先/外部URL/JAN羅列/パンくず等のノイズは削除。
- 「規格/サイズ/容量/素材・成分/内容量/セット内容」などは可能な限り<table>のKey-Valueに整理。
- 明確な列挙は<ul><li>に変換。
- それ以外は自然な文で<p>として要約（機能・特徴・使い方など）。
- 重複や同義反復は圧縮（例：「特徴特徴」→「特徴」）。
- 架空の情報は作らない。外部リンクを生成しない。
- 出力は断片のみ（<html> 等は不要）。**<section>以外の先頭テキストを出力しない。**
- 箇条書きの<li>テキスト先頭に『・』『●』『•』『-』『*』等の装飾記号を入れない（HTMLの<ul>側で表現する）。
- **注意書き・謝罪・指示文・プレースホルダー文（例：「入力が必要です。原文を入力してください。」）は出力しない。**
""";

        final String user = """
[チャンク #%d]
原文:
%s

出力要件:
- 出力はHTML断片のみ（<section> + <table>/<ul>/<p>）
- 表にできない場合は<table>省略可
- 冗長/重複語を整理し読みやすく
- 日本語の自然な文体
""".formatted(chunkIndex + 1, chunk);

        return groq.chat(
                model,
                List.of(Message.sys(system), Message.user(user)),
                maxTokens
        );
    }

    // ----- ここからヘルパ -----

    /** HTMLがあれば優先。無ければプレーン、最後にキャプション。 */
    private static String chooseBasePreferHtml(String html, String plain, String caption) {
        if (StringUtils.hasText(html)) return html;
        if (StringUtils.hasText(plain)) return plain;
        return caption != null ? caption : "";
    }

    /** 軽い正規化（エンティティ解除・改行統一） */
    private static String normalize(String s) {
        String t = (s == null) ? "" : HtmlUtils.htmlUnescape(s);
        t = t.replace("\r\n", "\n").replace("\r", "\n");
        return t.trim();
    }

    /** サイズ制御のための行単位分割 */
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

    /**
     * 先頭にゴミ文字が混入した場合などを除去
     * ・<section>より前は捨てる
     * ・よくある禁止文言を削除
     * ・<section>で始まらないならエラー
     */
    private static String sanitizeMerged(String html) {
        if (html == null) return "";
        String s = html;

        // <section> より前を削除
        s = s.replaceFirst("(?s)^\\s*[^<]*?(?=<section\\b)", "");

        // 禁止文言の追加除去
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

        // 最終的に<section>で始まらない場合は不正
        if (!s.trim().startsWith("<section")) {
            throw new IllegalStateException("LLM output did not start with <section> after sanitization.");
        }
        return s.trim();
    }

    /**
     * <li> 先頭に装飾記号が入っていないかの簡易検証
     */
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
