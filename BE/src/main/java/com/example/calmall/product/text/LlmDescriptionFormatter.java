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
import java.util.concurrent.atomic.AtomicLong;

/**
 * LLMで商品説明の整形
 */
@Slf4j
public class LlmDescriptionFormatter {

    private final GroqClient groq;
    private final String model;
    private final int maxTokens;
    private final int parallelism;

    // 文字数上限（≈2000 tokens 相当）
    private static final int MAX_INPUT_LENGTH = 3800;

    // 同時請求數上限（固定 2）
    private static final int MAX_CONCURRENT_REQUESTS = 2;

    // RateLimiter: 每次呼叫 Groq 至少間隔 (ms)
    private static final long MIN_CALL_INTERVAL_MS = 1200;
    private static final Semaphore RATE_LIMITER = new Semaphore(1, true);
    private static final AtomicLong LAST_CALL_TIME = new AtomicLong(0);

    // デフォルト並行数
    private static final int DEFAULT_PARALLELISM = 2;

    // === 新增：4參數建構子 ===
    public LlmDescriptionFormatter(GroqClient groq, String model, int maxTokens, int parallelism) {
        this.groq = groq;
        this.model = model;
        this.maxTokens = maxTokens;
        this.parallelism = parallelism <= 0 ? DEFAULT_PARALLELISM : parallelism;
    }

    // === 保留：3參數建構子（舊程式相容用） ===
    public LlmDescriptionFormatter(GroqClient groq, String model, int maxTokens) {
        this(groq, model, maxTokens, DEFAULT_PARALLELISM);
    }

    /** 原文（HTML/プレーン/キャプション/商品名）を LLM で整形 */
    public String cleanToHtml(String rawHtml, String rawPlain, String itemCaption, String itemName) {
        if (!StringUtils.hasText(rawHtml) && !StringUtils.hasText(rawPlain) && !StringUtils.hasText(itemCaption)) {
            return quotaExceededFallbackHtml(itemName);
        }
        return cleanToHtml(rawHtml, rawPlain, itemCaption);
    }

    /** 三參數版（既存互換用） */
    public String cleanToHtml(String rawHtml, String rawPlain, String itemCaption) {
        final String base = chooseBasePreferHtml(rawHtml, rawPlain, itemCaption);
        if (!StringUtils.hasText(base)) {
            throw new IllegalStateException("No source text to clean.");
        }

        final String normalized = truncateSafe(normalize(base), MAX_INPUT_LENGTH);
        final List<String> chunks = chunkSmart(normalized, 1800); // ← 適度に大きめに
        log.debug("[Groq LLM] chunk count={} (targetLen={})", chunks.size(), normalized.length());

        final int threads = Math.min(MAX_CONCURRENT_REQUESTS, Math.max(1, parallelism));
        final ExecutorService ex = Executors.newFixedThreadPool(threads);

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
                final String frag = f.get(90, TimeUnit.SECONDS);
                if (StringUtils.hasText(frag)) {
                    parts.add(frag);
                    ok++;
                } else {
                    ng++;
                }
            } catch (Exception e) {
                log.warn("[Groq LLM] chunk failed (execution)", e);
                ng++;
            }
        }
        log.debug("[Groq LLM] finished: success={} failures={}", ok, ng);

        if (ng > 0 || parts.isEmpty()) {
            return quotaExceededFallbackHtml(itemCaption);
        }

        final String merged = String.join("", parts);
        final String sanitized = sanitizeMerged(merged, itemCaption);
        assertNoLeadingBulletMarks(sanitized);
        return sanitized;
    }

    private String callGroqOnceWithRetry(int chunkIndex, String chunk) throws IOException, InterruptedException {
        int attempt = 0;
        IOException last = null;

        while (attempt < 3) {
            try {
                return callGroq(chunkIndex, chunk);
            } catch (IOException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("rate_limit_exceeded")) {
                    long wait = (long) Math.pow(2, attempt) * 2000;
                    log.warn("[Groq LLM] 429（chunk#{} attempt#{}）→ sleep {}ms", chunkIndex + 1, attempt + 1, wait);
                    Thread.sleep(wait);
                    last = e;
                } else {
                    long wait = (long) Math.pow(2, attempt) * 800;
                    log.warn("[Groq LLM] chunk#{} attempt#{} failed: {} → sleep {}ms",
                            chunkIndex + 1, attempt + 1, e.toString(), wait);
                    Thread.sleep(wait);
                    last = e;
                }
                attempt++;
            }
        }
        throw last != null ? last : new IOException("Groq call failed");
    }

    private String callGroq(int chunkIndex, String chunk) throws IOException, InterruptedException {
        enforceRateLimit();

        final String system = """
あなたはECサイト向けの「商品説明テキストの構造化クリーナー」です。
入力は雑多・重複・順序崩れを含む可能性があります。
以下のルールに従い、**安全な HTML 本文断片のみ**を出力してください。

【許可される要素】
<section class="desc-section table|bullets|body">、<table><tr><th|td>、<ul><li>、<p>

【変換ルール】
- 広告/クーポン/ショップ案内/FAQ/返品・交換/営業時間/連絡先/外部URL/JAN羅列は削除
- 「規格/サイズ/容量/素材・成分/内容量/セット内容」などは<table>に整理
- 箇条書きは<ul><li>
- それ以外は<p>に要約
- 重複や同義語は圧縮
- 架空の情報や外部リンクは禁止
- 出力は **<section> から始まるHTML断片のみ**
- <li> の先頭に装飾記号は入れない
- システム/ユーザー指示文やプレースホルダー文は出力禁止
""";

        final String user = """
[チャンク #%d]
原文:
%s

出力要件:
- <section> で始まる本文断片のみ
- 表にできない場合は<table>省略可
- 冗長/重複を整理し自然な日本語に
""".formatted(chunkIndex + 1, chunk);

        return groq.chat(
                model,
                List.of(Message.sys(system), Message.user(user)),
                maxTokens
        );
    }

    private static void enforceRateLimit() throws InterruptedException {
        RATE_LIMITER.acquire();
        try {
            long now = System.currentTimeMillis();
            long last = LAST_CALL_TIME.get();
            long elapsed = now - last;

            if (elapsed < MIN_CALL_INTERVAL_MS) {
                long wait = MIN_CALL_INTERVAL_MS - elapsed;
                log.debug("[Groq LLM] RateLimiter sleep {}ms", wait);
                Thread.sleep(wait);
            }

            LAST_CALL_TIME.set(System.currentTimeMillis());
        } finally {
            RATE_LIMITER.release();
        }
    }

    private static String chooseBasePreferHtml(String html, String plain, String caption) {
        if (StringUtils.hasText(html)) return html;
        if (StringUtils.hasText(plain)) return plain;
        return caption != null ? caption : "";
    }

    private static String normalize(String s) {
        String t = (s == null) ? "" : HtmlUtils.htmlUnescape(s);
        t = t.replace("\r\n", "\n").replace("\r", "\n");
        return t.trim();
    }

    private static String truncateSafe(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "\n※ これ以上の説明文は長すぎるため省略しました。";
    }

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

    private static String sanitizeMerged(String html, String itemName) {
        if (html == null) return "";
        String s = html;
        s = s.replaceFirst("(?s)^\\s*[^<]*?(?=<section\\b)", "");
        String[] banPhrases = {
                "入力が必要です", "please provide input", "no input provided",
                "placeholder", "これはテストです", "商品の詳細情報はありません。"
        };
        for (String bad : banPhrases) {
            s = s.replace(bad, "");
        }
        if (!s.trim().startsWith("<section")) {
            return "<section class=\"desc-section body\"><p>" +
                    (itemName != null ? itemName + " の商品説明は登録されていません。" : "商品説明は登録されていません。") +
                    "</p></section>";
        }
        return s.trim();
    }

    private static void assertNoLeadingBulletMarks(String html) {
        var m = java.util.regex.Pattern
                .compile("<li>\\s*([・●•\\-*])", java.util.regex.Pattern.DOTALL)
                .matcher(html);
        if (m.find()) {
            throw new IllegalStateException("LLM bullet formatting violated: found leading bullet marks inside <li>.");
        }
    }

    private static String quotaExceededFallbackHtml(String itemName) {
        return "<section class=\"desc-section body\"><p>" +
                (itemName != null ? itemName + " の商品説明は表示できません。" : "商品説明は表示できません。") +
                "（Groq の1日あたりのトークン上限を超過しました）</p></section>";
    }
}
