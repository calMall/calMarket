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

    public LlmDescriptionFormatter(GroqClient groq, String model, int maxTokens, int parallelism) {
        this.groq = groq;
        this.model = model;
        this.maxTokens = maxTokens;
        this.parallelism = parallelism;
    }

    /**
     * 原文（HTML/プレーン/キャプション）を LLM で整形
     */
    public String cleanToHtml(String rawHtml, String rawPlain, String itemCaption) {
        final String base = chooseBasePreferHtml(rawHtml, rawPlain, itemCaption);
        if (!StringUtils.hasText(base)) {
            throw new IllegalStateException("No source text to clean.");
        }

        // 入力正規化 ＋ 最大長で切り捨て
        final String normalized = truncateSafe(normalize(base), MAX_INPUT_LENGTH);

        // --- チャンク分割（800文字単位） ---
        final List<String> chunks = chunkSmart(normalized, 800);
        log.debug("[Groq LLM] chunks={}", chunks.size());

        // 並列実行
        final ExecutorService ex = Executors.newFixedThreadPool(Math.max(1, parallelism));
        final List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            final int idx = i;
            futures.add(ex.submit(() -> callGroqOnceWithRetry(idx, chunks.get(idx))));
        }
        ex.shutdown();

        // 出力収集
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
                log.debug("[Groq LLM] chunk failed (execution)", e);
                ng++;
            }
        }
        log.debug("[Groq LLM] finished: success={} failures={}", ok, ng);

        if (parts.isEmpty()) {
            // すべて失敗 → quota exceeded fallback
            return quotaExceededFallbackHtml(itemCaption);
        }

        // 連結 → サニタイズ → 簡易検証
        final String merged = String.join("", parts);
        final String sanitized = sanitizeMerged(merged, itemCaption);
        assertNoLeadingBulletMarks(sanitized);
        return sanitized;
    }

    // --- Groq 呼び出し (指数バックオフ) ---
    private String callGroqOnceWithRetry(int chunkIndex, String chunk) throws IOException, InterruptedException {
        int attempt = 0;
        long backoff = 800;
        IOException last = null;

        while (attempt < 3) {
            try {
                return callGroq(chunkIndex, chunk);
            } catch (IOException e) {
                // Groq 配額切れ (429) をキャッチしたら即 fallback
                if (e.getMessage() != null && e.getMessage().contains("rate_limit_exceeded")) {
                    log.warn("[Groq LLM] Token quota exceeded. Returning fallback.");
                    return quotaExceededFallbackHtml(null);
                }
                log.debug("[Groq LLM] chunk#{} attempt#{} failed: {}", chunkIndex + 1, attempt + 1, e.toString());
                last = e;
                Thread.sleep(backoff);
                backoff *= 2;
                attempt++;
            }
        }
        throw last != null ? last : new IOException("Groq call failed");
    }

    // --- system / user プロンプト (CSSなし) ---
    private String callGroq(int chunkIndex, String chunk) throws IOException {
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

    // --- Utility ---

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

    /** LLM出力のサニタイズ + 永久説明なしフィルタ */
    private static String sanitizeMerged(String html, String itemName) {
        if (html == null) return "";
        String s = html;

        // <section>より前を削除
        s = s.replaceFirst("(?s)^\\s*[^<]*?(?=<section\\b)", "");

        // 禁止フレーズ削除
        String[] banPhrases = {
                "入力が必要です", "please provide input", "no input provided",
                "placeholder", "これはテストです"
        };
        for (String bad : banPhrases) {
            s = s.replace(bad, "");
        }

        // === 永久説明なしフィルタ ===
        s = s.replaceAll("(?i)(入力.*ありません。?|説明.*ありません。?|no description.*|not provided.*)", "");

        if (!s.trim().startsWith("<section")) {
            // 全部削れたら fallback
            return "<section class=\"desc-section body\"><p>" +
                    (itemName != null ? itemName + " の商品説明は登録されていません。" : "商品説明は登録されていません。") +
                    "</p></section>";
        }

        return s.trim();
    }

    /** 箇条書き先頭に装飾記号が無いか確認 */
    private static void assertNoLeadingBulletMarks(String html) {
        var m = java.util.regex.Pattern
                .compile("<li>\\s*([・●•\\-*])", java.util.regex.Pattern.DOTALL)
                .matcher(html);
        if (m.find()) {
            throw new IllegalStateException("LLM bullet formatting violated: found leading bullet marks inside <li>.");
        }
    }

    /** Groq 配額切れ fallback */
    private static String quotaExceededFallbackHtml(String itemName) {
        return "<section class=\"desc-section body\"><p>" +
                (itemName != null ? itemName + " の商品説明は表示できません。" : "商品説明は表示できません。") +
                "（Groq の1日あたりのトークン上限を超過しました）</p></section>";
    }
}
