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

    // 入力文字数上限（≒2000 tokens）
    private static final int MAX_INPUT_LENGTH = 3800;

    // 同時実行上限（固定 2）
    private static final int MAX_CONCURRENT_REQUESTS = 2;

    // RateLimiter: 呼び出し最小間隔(ms)
    private static final long MIN_CALL_INTERVAL_MS = 1200;
    private static final Semaphore RATE_LIMITER = new Semaphore(1, true);
    private static final AtomicLong LAST_CALL_TIME = new AtomicLong(0);

    // 既存の 3 引数コンストラクタ（後方互換）
    public LlmDescriptionFormatter(GroqClient groq, String model, int maxTokens) {
        this(groq, model, maxTokens, 1);
    }

    // 新規 4 引数コンストラクタ（並列度指定）
    public LlmDescriptionFormatter(GroqClient groq, String model, int maxTokens, int parallelism) {
        this.groq = groq;
        this.model = model;
        this.maxTokens = maxTokens;
        this.parallelism = parallelism;
    }

    /** 4引数版（itemName まで渡す） */
    public String cleanToHtml(String rawHtml, String rawPlain, String itemCaption, String itemName) {
        if (!StringUtils.hasText(rawHtml) && !StringUtils.hasText(rawPlain) && !StringUtils.hasText(itemCaption)) {
            return quotaExceededFallbackHtml(itemName);
        }
        return cleanToHtml(rawHtml, rawPlain, itemCaption);
    }

    /** 3引数版（既存互換） */
    public String cleanToHtml(String rawHtml, String rawPlain, String itemCaption) {
        final String base = chooseBasePreferHtml(rawHtml, rawPlain, itemCaption);
        if (!StringUtils.hasText(base)) {
            throw new IllegalStateException("No source text to clean.");
        }

        final String normalized = truncateSafe(normalize(base), MAX_INPUT_LENGTH);

        // チャンクは大きめにしてリクエスト数を減らす（429対策）
        final int targetLen = 1200;
        final List<String> chunks = chunkSmart(normalized, targetLen);
        log.debug("[Groq LLM] chunk count={} (targetLen={})", chunks.size(), targetLen);

        // 固定最大同時 2
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

        // 部分成功は不採用 → Fallback
        if (ng > 0 || parts.isEmpty()) {
            return quotaExceededFallbackHtml(itemCaption);
        }

        final String merged = String.join("", parts);
        final String sanitized = sanitizeMerged(merged, itemCaption);
        assertNoLeadingBulletMarks(sanitized);
        return sanitized;
    }

    /** Retry/Backoff */
    private String callGroqOnceWithRetry(int chunkIndex, String chunk) throws IOException, InterruptedException {
        int attempt = 0;
        IOException last = null;

        while (attempt < 3) {
            try {
                return callGroq(chunkIndex, chunk);
            } catch (IOException e) {
                final String msg = e.getMessage() != null ? e.getMessage() : "";

                // 日次トークン上限に当たったら即座に失敗扱い（再試行しない）
                if (msg.contains("tokens per day")) {
                    log.warn("[Groq LLM] daily token quota exceeded → no retry (chunk#{})", chunkIndex + 1);
                    throw new IOException("GROQ_TPD_EXCEEDED");
                }

                if (msg.contains("rate_limit_exceeded")) {
                    long wait = (long) Math.pow(2, attempt) * 2000; // 2s → 4s → 8s
                    log.warn("[Groq LLM] 429（chunk#{} attempt#{}）→ sleep {}ms", chunkIndex + 1, attempt + 1, wait);
                    Thread.sleep(wait);
                    last = e;
                } else {
                    long wait = (long) Math.pow(2, attempt) * 800; // 0.8s → 1.6s → 3.2s
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

    /** Groq 呼び出し */
    private String callGroq(int chunkIndex, String chunk) throws IOException, InterruptedException {
        enforceRateLimit();

        final String system = """
あなたはECサイト向けの「商品説明テキストの構造化クリーナー」です。
以下のルールに従い、安全な HTML 本文断片のみを出力してください。
【許可要素】<section class="desc-section ...">, <table><tr><th|td>, <ul><li>, <p>
【禁止】外部リンク, 架空情報, プレースホルダー
""";

        final String user = """
[チャンク #%d]
原文:
%s
出力要件:
- <section> で始まる本文断片のみ
- 冗長/重複を整理し自然な日本語に
""".formatted(chunkIndex + 1, chunk);

        return groq.chat(
                model,
                List.of(Message.sys(system), Message.user(user)),
                maxTokens
        );
    }

    /** RateLimiter */
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

    // ===== Utility =====

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

    /** マージ後の妥当性チェック */
    private static String sanitizeMerged(String html, String itemName) {
        if (html == null) return "";
        String s = html.replaceFirst("(?s)^\\s*[^<]*?(?=<section\\b)", "");
        if (!s.trim().startsWith("<section")) {
            return quotaExceededFallbackHtml(itemName);
        }
        return s.trim();
    }

    /** <li>の直下に装飾記号を禁止 */
    private static void assertNoLeadingBulletMarks(String html) {
        var m = java.util.regex.Pattern
                .compile("<li>\\s*([・●•\\-*])", java.util.regex.Pattern.DOTALL)
                .matcher(html);
        if (m.find()) {
            throw new IllegalStateException("LLM bullet formatting violated.");
        }
    }

    /** Fallback（DB 保存回避用マーカー付き） */
    private static String quotaExceededFallbackHtml(String itemName) {
        return "<!--__GROQ_FALLBACK__-->" +
                "<section class=\"desc-section body\"><p>" +
                (itemName != null ? itemName + " の商品説明は表示できません。" : "商品説明は表示できません。") +
                "（Groq の1日あたりのトークン上限を超過しました）</p></section>";
    }
}
