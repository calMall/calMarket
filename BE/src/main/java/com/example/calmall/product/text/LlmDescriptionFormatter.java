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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM で商品説明を安全な HTML に整形するユーティリティ
 */
@Slf4j
public class LlmDescriptionFormatter {

    private final GroqClient groq;
    private final String model;
    private final int maxTokens;
    private final int parallelism;

    // 入力テキストの最大長（概ね 2k tokens 相当）
    private static final int MAX_INPUT_LENGTH = 3800;

    // 1 回の整形で許容する最大チャンク数（根治策 ：過分割禁止）
    private static final int MAX_CHUNKS = 2;

    // 並列実行は 1（逐次実行）。429 の根源を断つ。
    private static final int MAX_CONCURRENT_REQUESTS = 1;

    // RateLimiter：Groq 呼び出し間隔（ms）
    private static final long MIN_CALL_INTERVAL_MS = 1500L;
    private static final Semaphore RATE_LIMITER = new Semaphore(1, true);
    private static final AtomicLong LAST_CALL_TIME = new AtomicLong(0);

    // 429 / 一般エラーの最大リトライ回数
    private static final int MAX_RETRY = 3;

    // 429 メッセージから「再試行までの秒数」を推定するための正規表現
    private static final Pattern RETRY_IN_SECONDS = Pattern.compile("try again in ([0-9]+(?:\\.[0-9]+)?)s", Pattern.CASE_INSENSITIVE);

    public LlmDescriptionFormatter(GroqClient groq, String model, int maxTokens, int parallelism) {
        this.groq = groq;
        this.model = model;
        this.maxTokens = maxTokens;
        this.parallelism = parallelism;
    }

    /**
     * 4 引数版（商品名はフォールバック文言に使用）
     */
    public String cleanToHtml(String rawHtml, String rawPlain, String itemCaption, String itemName) {
        if (!StringUtils.hasText(rawHtml) && !StringUtils.hasText(rawPlain) && !StringUtils.hasText(itemCaption)) {
            return quotaExceededFallbackHtml(itemName);
        }
        return cleanToHtml(rawHtml, rawPlain, itemCaption);
    }

    /**
     * 3 引数版（互換用）
     */
    public String cleanToHtml(String rawHtml, String rawPlain, String itemCaption) {
        final String base = chooseBasePreferHtml(rawHtml, rawPlain, itemCaption);
        if (!StringUtils.hasText(base)) {
            throw new IllegalStateException("入力テキストが空です。");
        }

        // 正規化と安全な切り詰め
        final String normalized = truncateSafe(normalize(base), MAX_INPUT_LENGTH);

        // 過分割を避けるため、長さに応じてターゲット長を自動調整（最大チャンク数 2）
        final int targetLen = Math.max(600, (int)Math.ceil((double)normalized.length() / MAX_CHUNKS));
        final List<String> chunks = chunkSmart(normalized, targetLen);
        log.debug("[Groq LLM] chunk count={} (targetLen={})", chunks.size(), targetLen);

        // 逐次実行（並列 1）
        final ExecutorService ex = Executors.newFixedThreadPool(
                Math.min(MAX_CONCURRENT_REQUESTS, Math.max(1, parallelism))
        );

        final List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            final int idx = i;
            futures.add(ex.submit(() -> callGroqOnceWithRetry(idx, chunks.get(idx))));
        }
        ex.shutdown();

        // 全チャンクが成功しない限り、結果は採用しない（部分成功は禁止）
        final List<String> parts = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                final String frag = futures.get(i).get(120, TimeUnit.SECONDS);
                if (!StringUtils.hasText(frag)) {
                    log.warn("[Groq LLM] chunk#{} が空出力のため不採用", i + 1);
                    return quotaExceededFallbackHtml(null);
                }
                parts.add(frag);
            } catch (Exception e) {
                log.warn("[Groq LLM] chunk#{} 取得に失敗（部分成功は不採用）: {}", i + 1, e.toString());
                return quotaExceededFallbackHtml(null);
            }
        }

        // 連結 → サニタイズ → 体裁検証
        final String merged = String.join("", parts);
        final String sanitized = sanitizeMerged(merged, null);
        assertNoLeadingBulletMarks(sanitized);
        return sanitized;
    }

    /**
     * Groq 呼び出しの単発試行（指数バックオフ＋429 はメッセージから待機秒を推定）
     */
    private String callGroqOnceWithRetry(int chunkIndex, String chunk) throws IOException, InterruptedException {
        IOException last = null;

        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            try {
                return callGroq(chunkIndex, chunk);
            } catch (IOException e) {
                last = e;
                final String msg = e.getMessage() != null ? e.getMessage() : "";

                if (msg.contains("rate_limit_exceeded")) {
                    // 429：サーバーメッセージから秒数を推定して待機（なければ指数バックオフ）
                    long sleepMs = extractRetryAfterMs(msg);
                    if (sleepMs <= 0) {
                        sleepMs = (long) Math.pow(2, attempt) * 2000L; // 2s, 4s, 8s
                    }
                    log.warn("[Groq LLM] 429（chunk#{} attempt#{}）→ sleep {}ms", chunkIndex + 1, attempt + 1, sleepMs);
                    Thread.sleep(sleepMs);
                } else {
                    // 一般エラー：指数バックオフ
                    long sleepMs = (long) Math.pow(2, attempt) * 1000L; // 1s, 2s, 4s
                    log.warn("[Groq LLM] エラー（chunk#{} attempt#{}）: {} → sleep {}ms",
                            chunkIndex + 1, attempt + 1, e.toString(), sleepMs);
                    Thread.sleep(sleepMs);
                }
            }
        }
        throw last != null ? last : new IOException("Groq 呼び出しに失敗しました。");
    }

    /**
     * 実際の Groq 呼び出し（RateLimiter で最小間隔を保証）
     */
    private String callGroq(int chunkIndex, String chunk) throws IOException, InterruptedException {
        enforceRateLimit();

        final String system = """
あなたはECサイト向けの「商品説明テキストの構造化クリーナー」です。
入力は雑多・重複・順序崩れを含む可能性があります。
次のルールに従い、**安全な HTML 本文断片のみ**を出力してください。

【許可される要素】
<section class="desc-section table|bullets|body">、<table><tr><th|td>、<ul><li>、<p>

【変換ルール】
- 広告/クーポン/ショップ案内/FAQ/返品・交換/営業時間/連絡先/外部URL/JAN羅列は削除
- 「規格/サイズ/容量/素材・成分/内容量/セット内容」などは<table>へ整理
- 箇条書きは<ul><li>、その他は<p>に要約
- 重複や同義語は圧縮
- 架空情報や外部リンクは禁止
- 出力は **<section> から始まる HTML 断片のみ**
- <li> の先頭に装飾記号を付けない
- システム/ユーザー指示文やプレースホルダー文を出力しない
""";

        final String user = """
[チャンク #%d]
原文:
%s

出力要件:
- <section> で始まる本文断片のみ
- 表にできない内容は<table>省略可
- 冗長/重複を整理し自然な日本語に
""".formatted(chunkIndex + 1, chunk);

        return groq.chat(
                model,
                List.of(Message.sys(system), Message.user(user)),
                maxTokens
        );
    }

    /**
     * RateLimiter：グローバルに最小呼び出し間隔を保証
     */
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

    /**
     * 429 メッセージから推奨待機秒を抽出（見つからなければ 0）
     */
    private static long extractRetryAfterMs(String message) {
        try {
            Matcher m = RETRY_IN_SECONDS.matcher(message);
            if (m.find()) {
                double sec = Double.parseDouble(m.group(1));
                // 安全側に 10% 余裕を積む
                return (long) Math.ceil(sec * 1100.0);
            }
        } catch (Exception ignore) {}
        return 0;
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

    /**
     * 行単位でスマートに分割。targetLen を超える前に切る。
     */
    private static List<String> chunkSmart(String text, int targetLen) {
        final List<String> lines = Arrays.stream(text.split("\n"))
                .map(String::trim).filter(l -> !l.isEmpty()).toList();

        final List<String> out = new ArrayList<>();
        final StringBuilder buf = new StringBuilder(targetLen + 256);
        for (String ln : lines) {
            if (buf.length() + ln.length() + 1 > targetLen) {
                if (buf.length() > 0) out.add(buf.toString().trim());
                buf.setLength(0);
            }
            if (buf.length() > 0) buf.append('\n');
            buf.append(ln);
        }
        if (buf.length() > 0) out.add(buf.toString().trim());

        // ガード：最大チャンク数を上限に抑える（超過時は最後の要素に詰める）
        if (out.size() > MAX_CHUNKS) {
            StringBuilder mergeTail = new StringBuilder();
            for (int i = MAX_CHUNKS - 1; i < out.size(); i++) {
                if (mergeTail.length() > 0) mergeTail.append('\n');
                mergeTail.append(out.get(i));
            }
            List<String> capped = new ArrayList<>(out.subList(0, MAX_CHUNKS - 1));
            capped.add(mergeTail.toString());
            return capped;
        }
        return out;
    }

    /**
     * LLM 出力のサニタイズ。プレースホルダー除去と <section> 先頭保証。
     */
    private static String sanitizeMerged(String html, String itemName) {
        if (html == null) return "";
        String s = html;

        // <section> より前のノイズを除去
        s = s.replaceFirst("(?s)^\\s*[^<]*?(?=<section\\b)", "");

        // 禁止フレーズ除去
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

    /**
     * 箇条書きの先頭に装飾記号が混入していないかの検証
     */
    private static void assertNoLeadingBulletMarks(String html) {
        var m = java.util.regex.Pattern
                .compile("<li>\\s*([・●•\\-*])", java.util.regex.Pattern.DOTALL)
                .matcher(html);
        if (m.find()) {
            throw new IllegalStateException("箇条書きの先頭に装飾記号が検出されました。");
        }
    }

    /**
     * フォールバック HTML（トークン上限等で LLM が使えない場合）
     */
    private static String quotaExceededFallbackHtml(String itemName) {
        return "<section class=\"desc-section body\"><p>" +
                (itemName != null ? itemName + " の商品説明は表示できません。" : "商品説明は表示できません。") +
                "（LLM のトークン上限に達しました）</p></section>";
    }
}
