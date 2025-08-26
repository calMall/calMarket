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
 * LLMで商品説明の整形（CSSなし・堅牢版）
 * - 入力正規化
 * - 最大入力長の強制（安全切断）
 * - 800文字前後のスマート分割（最大チャンク数 6）
 * - 控えめ並列（推奨 2〜3）
 * - Groq 429（TPD超過）時は商品名を使った日本語のフォールバックHTMLを返す
 * - sanitizer で「商品説明はありません。」や空殻 body セクションを除去
 * - <li>先頭の装飾記号チェック
 */
@Slf4j
public class LlmDescriptionFormatter {

    // === トークン/日 目標に合わせた安全設計値 ===
    private static final int MAX_INPUT_LENGTH = 3800; // ≈ ~2,000 tokens を目安
    private static final int CHUNK_SIZE = 800;        // 800字で分割
    private static final int MAX_CHUNKS = 6;          // 最大 6 チャンク（合計 ≒ 4,800字までだが先に3800でtruncate）

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
     * 原文（HTML/プレーン/キャプション）を LLM で整形し、<section> から始まる HTML 断片を返す。
     * 429（TPD超過）が発生した場合は「商品名 + 配額切れの説明」のフォールバック HTML を返す。
     */
    public String cleanToHtml(String rawHtml, String rawPlain, String itemCaption, String itemNameForQuotaFallback) {
        final String base = chooseBasePreferHtml(rawHtml, rawPlain, itemCaption);
        if (!StringUtils.hasText(base)) {
            throw new IllegalStateException("No source text to clean.");
        }

        // 1) 入力正規化 + 最大長で安全切断
        final String normalized = normalize(base);
        final String limited = truncateSafe(normalized, MAX_INPUT_LENGTH);

        // 2) 800字スマート分割（最大 MAX_CHUNKS）
        final List<String> chunks = chunkSmart(limited, CHUNK_SIZE, MAX_CHUNKS);
        log.debug("[Groq LLM] chunks={}", chunks.size());

        // 3) 控えめ並列で LLM 呼び出し
        final ExecutorService ex = Executors.newFixedThreadPool(Math.max(1, parallelism));
        final List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            final int idx = i;
            futures.add(ex.submit(() -> callGroqOnceWithRetry(idx, chunks.get(idx))));
        }
        ex.shutdown();

        // 4) 出力収集
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
            } catch (ExecutionException ee) {
                // Groq 429（TPD超過）はここに入る可能性が高いので検知
                Throwable cause = ee.getCause();
                if (cause instanceof IOException io && isGroqDailyQuotaExceeded(io)) {
                    log.warn("[Groq LLM] Daily quota exceeded. Returning fallback HTML.");
                    return quotaExceededFallbackHtml(itemNameForQuotaFallback);
                }
                log.debug("[Groq LLM] chunk failed (execution)", ee);
                ng++;
            } catch (Exception e) {
                log.debug("[Groq LLM] chunk failed (unknown)", e);
                ng++;
            }
        }
        log.debug("[Groq LLM] finished: success={} failures={}", ok, ng);

        if (parts.isEmpty()) {
            // 429 以外の理由で全滅した場合は例外化（呼び出し元でDBフォールバックなど）
            throw new IllegalStateException("LLM produced no fragments (all chunks failed).");
        }

        // 5) 連結 → サニタイズ → 簡易検証
        final String merged = String.join("", parts);
        final String sanitized = sanitizeMerged(merged);
        assertNoLeadingBulletMarks(sanitized);
        return sanitized;
    }

    // ===== Groq 呼び出し（指数バックオフ） =====

    private String callGroqOnceWithRetry(int chunkIndex, String chunk) throws IOException, InterruptedException {
        int attempt = 0;
        long backoff = 800; // ms
        IOException last = null;

        while (attempt < 3) {
            try {
                return callGroq(chunkIndex, chunk);
            } catch (IOException e) {
                if (isGroqDailyQuotaExceeded(e)) {
                    // TPD超過はすぐに投げ返して上位でフォールバック
                    throw e;
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

    // system / user プロンプト（CSSなし）
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
- **「商品説明はありません。」のような空文・案内文は出力しない**
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

    private static boolean isGroqDailyQuotaExceeded(IOException e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        return msg.contains("Rate limit reached")
                && msg.contains("tokens per day (TPD)")
                && msg.contains("rate_limit_exceeded");
    }

    // ===== Utility =====

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

    /** 安全切断（サロゲートペア・結合文字を壊さない / 末尾に省略注記） */
    private static String truncateSafe(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        // 末尾のコードポイント境界で切る
        int end = text.offsetByCodePoints(0, Math.max(0, text.codePointCount(0, maxLen)));
        String cut = text.substring(0, end).trim();
        // 極端に短い場合の保険
        if (cut.isEmpty()) {
            end = Math.min(text.length(), maxLen);
            cut = text.substring(0, end).trim();
        }
        return cut + "\n\n※ 元の説明文が長いため、内容を一部省略して整形しています。";
    }

    /** サイズ制御のための行単位分割（targetLen 超なら改行境界で積み上げ） */
    private static List<String> chunkSmart(String text, int targetLen, int maxChunks) {
        final List<String> lines = Arrays.stream(text.split("\n"))
                .map(String::trim).filter(l -> !l.isEmpty()).toList();

        final List<String> out = new ArrayList<>();
        final StringBuilder buf = new StringBuilder(targetLen + 256);
        for (String ln : lines) {
            if (buf.length() + ln.length() + 1 > targetLen) {
                out.add(buf.toString().trim());
                if (out.size() >= maxChunks) break;
                buf.setLength(0);
            }
            if (buf.length() > 0) buf.append('\n');
            buf.append(ln);
        }
        if (buf.length() > 0 && out.size() < maxChunks) out.add(buf.toString().trim());
        return out;
    }

    /** LLM出力のサニタイズ（禁止文言／空殻 body セクションを除去） */
    private static String sanitizeMerged(String html) {
        if (html == null) return "";
        String s = html;

        // <section>より前を削除
        s = s.replaceFirst("(?s)^\\s*[^<]*?(?=<section\\b)", "");

        // 禁止文言（既存＋追加）
        String[] banPhrases = {
                "入力が必要です。原文を入力してください。",
                "please provide input",
                "no input provided",
                "placeholder",
                "これはテストです",
                "商品説明はありません。",
                "商品説明はありません"
        };
        for (String bad : banPhrases) {
            s = s.replace(bad, "");
        }
        
        s = s.replaceAll("(?is)<p>\\s*</p>", "");

        // body セクションが空なら section ごと削除
        s = s.replaceAll(
                "(?is)<section\\s+class=\"[^\"]*\\bdesc-section\\b[^\"<>]*\\bbody\\b[^\"]*\"[^>]*>\\s*</section>\\s*",
                ""
        );

        if (!s.trim().startsWith("<section")) {
            throw new IllegalStateException("LLM output did not start with <section> after sanitization.");
        }
        return s.trim();
    }

    /** <li> 先頭に装飾記号が入っていないかの簡易検証 */
    private static void assertNoLeadingBulletMarks(String html) {
        var m = java.util.regex.Pattern
                .compile("<li>\\s*([・●•\\-*])", java.util.regex.Pattern.DOTALL)
                .matcher(html);
        if (m.find()) {
            throw new IllegalStateException(
                    "LLM bullet formatting violated: found leading bullet marks inside <li>."
            );
        }
    }

    // GroqのTPDが尽きたときのフォールバックHTML
    private static String quotaExceededFallbackHtml(String itemName) {
        String safeName = StringUtils.hasText(itemName) ? HtmlUtils.htmlEscape(itemName) : "この商品";
        return """
<section class="desc-section body">
  <p>%s の詳細な説明文を生成できませんでした。</p>
  <p>理由：Groq の毎日トークン配額（TPD 100,000）が上限に達しました。日次配額が回復すると自動的に生成されます。</p>
  <p>※ 商品の基本情報（名称・価格・画像など）は引き続き表示しています。</p>
</section>
""".formatted(safeName).trim();
    }
}
