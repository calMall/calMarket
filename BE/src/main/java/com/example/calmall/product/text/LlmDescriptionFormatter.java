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
 * LLM を用いた商品説明の整形（LLM 全任・後処理なし・最低限検証のみ）。
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

    /** 原文（HTML/プレーン/キャプション）→ LLM で構造化 HTML（LLM全任） */
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

        // LLM 出力を「そのまま」結合（後端での整形/抽出は一切しない）
        final String merged = String.join("", parts);

        // 軽い検証のみ：<li> の先頭に装飾記号を含めない（非修正・違反なら失敗）
        assertNoLeadingBulletMarks(merged);

        return merged;
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
                log.debug("[Groq LLM] chunk#{} attempt#{} failed: {}", chunkIndex + 1, attempt + 1, e.toString());
                last = e;
                Thread.sleep(backoff);
                backoff *= 2;
                attempt++;
            }
        }
        throw last != null ? last : new IOException("Groq call failed");
    }

    /** 日本語プロンプト：LLM に完全委譲（箇条書きの先頭記号禁止まで明示） */
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
- 箇条書きの <li> テキスト先頭に『・』『●』『•』『-』『*』等の装飾記号を入れない。装飾は HTML の <ul> のみで表現する。
- 箇条書きを使う場合は <section class="desc-section bullets"><ul>…</ul></section> を用い、各 <li> は素の文だけを書く。
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

    // -------------------- helpers（整形ではなく分割・検証のための最小限） --------------------

    /** HTML があれば優先。無ければプレーン、最後にキャプション。 */
    private static String chooseBasePreferHtml(String html, String plain, String caption) {
        if (StringUtils.hasText(html)) return html;
        if (StringUtils.hasText(plain)) return plain;
        return caption != null ? caption : "";
    }

    /** HTMLエンティティ解除・改行正規化（内容を改変しない範囲の最小化） */
    private static String normalize(String s) {
        String t = (s == null) ? "" : HtmlUtils.htmlUnescape(s);
        t = t.replace("\r\n", "\n").replace("\r", "\n");
        return t.trim();
    }

    /** 行ベースでチャンク化（LLM への入力サイズ制御；意味は変えない） */
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

    /** <li> の先頭に装飾記号が紛れ込んでいないかだけ検証（修正はしない） */
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
