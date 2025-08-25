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

@Slf4j
public class LlmDescriptionFormatter {

    private static final int MAX_INPUT_LENGTH = 3800; // 安全上限 ≈ 2,000 tokens
    private static final int CHUNK_SIZE = 800;        // 每段 800 字
    private static final int MAX_CHUNKS = 5;          // 最多 5 段 → 上限 ≈ 4,000 字內

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

    /** 原文（HTML/プレーン/キャプション）を LLM で整形して HTML 断片を返す */
    public String cleanToHtml(String rawHtml, String rawPlain, String itemCaption) {
        final String base = chooseBasePreferHtml(rawHtml, rawPlain, itemCaption);
        if (!StringUtils.hasText(base)) {
            throw new IllegalStateException("No source text to clean.");
        }

        // 正規化 + 安全切断（文途中の分割を避ける）
        final String normalized = normalize(base);
        final String bounded = truncateSafe(normalized, MAX_INPUT_LENGTH);

        // 800 字チャンク化（最大 MAX_CHUNKS）
        final List<String> chunks = chunkSmart(bounded, CHUNK_SIZE, MAX_CHUNKS);
        log.debug("[Groq LLM] chunks={}", chunks.size());

        // 控えめ並列
        final ExecutorService ex = Executors.newFixedThreadPool(Math.max(1, parallelism));
        final List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            final int idx = i;
            futures.add(ex.submit(() -> callGroqOnceWithRetry(idx, chunks.get(idx))));
        }
        ex.shutdown();

        // 回収
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
            throw new IllegalStateException("LLM produced no fragments (all chunks failed).");
        }

        // 連結 → サニタイズ → 検証
        final String merged = String.join("", parts);
        final String sanitized = sanitizeMerged(merged);
        assertNoLeadingBulletMarks(sanitized);
        return sanitized;
    }

    // --- Groq 呼び出し（指数バックオフ + 微小ジッター） ---
    private String callGroqOnceWithRetry(int chunkIndex, String chunk) throws IOException, InterruptedException {
        int attempt = 0;
        long backoff = 800;
        IOException last = null;

        while (attempt < 3) {
            try {
                return callGroq(chunkIndex, chunk);
            } catch (IOException e) {
                last = e;
                long jitter = ThreadLocalRandom.current().nextLong(100, 300);
                log.debug("[Groq LLM] chunk#{} attempt#{} failed: {} (sleep {}ms)", chunkIndex + 1, attempt + 1, e.toString(), backoff + jitter);
                Thread.sleep(backoff + jitter);
                backoff *= 2;
                attempt++;
            }
        }
        throw last != null ? last : new IOException("Groq call failed");
    }

    // --- system / user プロンプト（CSSなし） ---
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
- 箇条書きは<ul><li>、それ以外は<p>に要約
- 重複や同義語は圧縮、架空情報と外部リンクは禁止
- 出力は **<section> から始まるHTML断片のみ**
- <li> テキスト先頭に装飾記号（・●•-*）は入れない
- 指示文・謝罪・プレースホルダー文は出力禁止
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

    /** HTML > プレーン > キャプション の順に採用 */
    private static String chooseBasePreferHtml(String html, String plain, String caption) {
        if (StringUtils.hasText(html)) return html;
        if (StringUtils.hasText(plain)) return plain;
        return caption != null ? caption : "";
    }

    /** エンティティ解除 & 改行正規化 */
    private static String normalize(String s) {
        String t = (s == null) ? "" : HtmlUtils.htmlUnescape(s);
        t = t.replace("\r\n", "\n").replace("\r", "\n");
        return t.trim();
    }

    /** 文/行の境界を優先して安全に切る */
    private static String truncateSafe(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) return text == null ? "" : text;

        // 1) 句点/改行を探す（maxLen 付近から後ろ方向）
        int boundary = -1;
        for (int i = maxLen; i >= Math.max(0, maxLen - 200); i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '.' || c == '!' || c == '?' || c == '\n') {
                boundary = i + 1;
                break;
            }
        }
        // 2) なければ空白／全角空白で切る
        if (boundary == -1) {
            for (int i = maxLen; i >= Math.max(0, maxLen - 100); i--) {
                char c = text.charAt(i);
                if (Character.isWhitespace(c) || c == '　') {
                    boundary = i;
                    break;
                }
            }
        }
        // 3) それでも無ければ強制切り
        if (boundary == -1) boundary = maxLen;

        String head = text.substring(0, boundary).trim();
        String note = "\n\n※ 文章が長いため一部のみを整形しています。続きは省略されました。";
        return head + note;
    }

    /** 行ベースで targetLen ずつ結合、最大 maxChunks まで */
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

    /** <section> より前を除去 + 禁止文言削除 + 形式検証 */
    private static String sanitizeMerged(String html) {
        if (html == null) return "";
        String s = html;

        s = s.replaceFirst("(?s)^\\s*[^<]*?(?=<section\\b)", "");
        String[] banPhrases = {
                "入力が必要です。原文を入力してください。",
                "please provide input",
                "no input provided",
                "placeholder",
                "これはテストです"
        };
        for (String bad : banPhrases) s = s.replace(bad, "");

        if (!s.trim().startsWith("<section")) {
            throw new IllegalStateException("LLM output did not start with <section> after sanitization.");
        }
        return s.trim();
    }

    /** <li> 先頭に装飾記号が無いか検証 */
    private static void assertNoLeadingBulletMarks(String html) {
        var m = java.util.regex.Pattern
                .compile("<li>\\s*([・●•\\-*])", java.util.regex.Pattern.DOTALL)
                .matcher(html);
        if (m.find()) {
            throw new IllegalStateException("LLM bullet formatting violated: found leading bullet marks inside <li>.");
        }
    }
}
