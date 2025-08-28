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

    // 同時請求数上限（固定 2）
    private static final int MAX_CONCURRENT_REQUESTS = 2;

    // RateLimiter: 各 Groq 呼び出しの最小インターバル（ms）
    private static final long MIN_CALL_INTERVAL_MS = 1200;
    private static final Semaphore RATE_LIMITER = new Semaphore(1, true);
    private static final AtomicLong LAST_CALL_TIME = new AtomicLong(0);

    public LlmDescriptionFormatter(GroqClient groq, String model, int maxTokens) {
        this(groq, model, maxTokens, 2);
    }

    public LlmDescriptionFormatter(GroqClient groq, String model, int maxTokens, int parallelism) {
        this.groq = groq;
        this.model = model;
        this.maxTokens = maxTokens;
        this.parallelism = parallelism;
    }

    /** 原文（HTML/プレーン/キャプション/商品名）を LLM で整形（4引数） */
    public String cleanToHtml(String rawHtml, String rawPlain, String itemCaption, String itemName) {
        if (!StringUtils.hasText(rawHtml) && !StringUtils.hasText(rawPlain) && !StringUtils.hasText(itemCaption)) {
            return quotaExceededFallbackHtml(itemName);
        }
        return cleanToHtmlInternal(rawHtml, rawPlain, itemCaption, itemName);
    }

    /** 互換用（3引数） */
    public String cleanToHtml(String rawHtml, String rawPlain, String itemCaption) {
        return cleanToHtmlInternal(rawHtml, rawPlain, itemCaption, null);
    }

    private String cleanToHtmlInternal(String rawHtml, String rawPlain, String itemCaption, String itemName) {
        final String base = chooseBasePreferHtml(rawHtml, rawPlain, itemCaption);
        if (!StringUtils.hasText(base)) {
            throw new IllegalStateException("No source text to clean.");
        }

        // 事前フィルタ（パンくず/販促テンプレ等を除去）
        final String normalized = truncateSafe(prefilterGarbage(normalize(base)), MAX_INPUT_LENGTH);

        // 1発処理（短文）
        if (normalized.length() <= 1200) {
            log.debug("[Groq LLM] force single chunk for short input (len={})", normalized.length());
            try {
                final String merged = callGroqOnceWithRetry(0, normalized);
                final String sanitized = sanitizeMerged(merged, itemName != null ? itemName : itemCaption);
                assertNoLeadingBulletMarks(sanitized);
                return sanitized;
            } catch (Exception e) {
                log.warn("[Groq LLM] single chunk failed", e);
                return quotaExceededFallbackHtml(itemName != null ? itemName : itemCaption);
            }
        }

        // 分割並行
        final List<String> chunks = chunkSmart(normalized, 1600);
        log.debug("[Groq LLM] chunk count={} (targetLen=1600)", chunks.size());

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
                final String frag = f.get(90, TimeUnit.SECONDS); // timeout 90s
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
            return quotaExceededFallbackHtml(itemName != null ? itemName : itemCaption);
        }

        final String merged = String.join("", parts);
        final String sanitized = sanitizeMerged(merged, itemName != null ? itemName : itemCaption);
        assertNoLeadingBulletMarks(sanitized);
        return sanitized;
    }

    // === Retry / Backoff 策略 ===
    private String callGroqOnceWithRetry(int chunkIndex, String chunk) throws IOException, InterruptedException {
        int attempt = 0;
        IOException last = null;

        while (attempt < 3) {
            try {
                return callGroq(chunkIndex, chunk);
            } catch (IOException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("tokens per day") || msg.contains("TPD")) {
                    log.warn("[Groq LLM] daily token quota exceeded → no retry (chunk#{})", chunkIndex + 1);
                    throw new IOException("GROQ_TPD_EXCEEDED");
                }
                if (msg.contains("rate_limit_exceeded") || msg.contains("429")) {
                    long wait = (long) Math.pow(2, attempt) * 2000; // 2s → 4s → 8s
                    log.warn("[Groq LLM] 429（chunk#{} attempt#{}）→ sleep {}ms", chunkIndex + 1, attempt + 1, wait);
                    Thread.sleep(wait);
                    last = e;
                } else {
                    long wait = (long) Math.pow(2, attempt) * 800; // 0.8s → 1.6s → 3.2s
                    log.warn("[Groq LLM] chunk#{} attempt#{} failed: {} → sleep {}ms", chunkIndex + 1, attempt + 1, e.toString(), wait);
                    Thread.sleep(wait);
                    last = e;
                }
                attempt++;
            }
        }
        throw last != null ? last : new IOException("Groq call failed");
    }

    // === system / user prompt（DICT 付き / extractive-only） ===
    private String callGroq(int chunkIndex, String chunk) throws IOException, InterruptedException {
        enforceRateLimit();

        java.util.Set<String> dict = buildTermSet(chunk);

        final String system = """
あなたはECサイト向けの「商品説明テキストの構造化クリーナー」です。
出力は **抽出圧縮（extractive-only）** とし、原文に存在しない名詞・機能・数値を新規に作成してはいけません。
以下のルールに従い、**安全な HTML 本文断片のみ**を出力してください。

【許可要素】<section class="desc-section table|bullets|body">、<table><tr><th|td>、<ul><li>、<p>
【禁止】広告/店舗案内/外部URL/FAQ/連絡先/クーポン/メタ発言/空虚な定型句
【変換】規格や成分などの「項目：値」は<table>、箇条書きは<ul><li>、その他は<p>
【整形】重複統合・冗長削除・誤字は直さずに削除（推測で補完しない）
【出力】<section> から始まる断片のみ。前後に何も出力しない。
""";

        final String user = """
[チャンク #%d]
原文:
%s

【DICT（使用許可語彙）】
%s

【厳格制約】
- 各文は上記 DICT の語彙（または原文の数値・単位）を必ず1語以上含むこと。含まない文は出力しない。
- DICT にない新規名詞を導入しない。
- 内容が空になる場合は、次の1行のみを出力: <section class="desc-section body"><p></p></section>
- <li> の先頭に記号（・●•-*）を付けない。
""".formatted(chunkIndex + 1, chunk, String.join("、", dict));

        return groq.chat(model, List.of(Message.sys(system), Message.user(user)), maxTokens);
    }

    // === RateLimiter ===
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

    // === Utility ===

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

    /** LLM投入前の簡易ガーベジ除去 */
    private static String prefilterGarbage(String s) {
        if (s == null) return "";
        String t = s;

        // パンくず
        t = t.replaceAll("(?m)^\\s*お店TOP＞.*$", "");
        t = t.replaceAll("(?m)^\\s*カテゴリTOP＞.*$", "");

        // 販促・注意の定型
        t = t.replaceAll("(?m)^\\s*お一人様\\d+個.*$", "");
        t = t.replaceAll("(?m)^\\s*※?要エントリー.*$", "");
        t = t.replaceAll("(?m)^\\s*クーポン.*$", "");
        t = t.replaceAll("(?m)^\\s*ショップ(情報|案内).*$", "");

        // 空行圧縮
        t = t.replaceAll("(\\r?\\n){3,}", "\n\n");
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

    /** LLM出力のサニタイズ（DICT 驗收＋表化＋セクション整理） */
    private static String sanitizeMerged(String html, String itemName) {
        if (html == null) return "";
        String s = html;

        // <section> までの前置きを除去
        s = s.replaceFirst("(?s)^\\s*[^<]*?(?=<section\\b)", "");

        // body セクション内の「キー：値」パラグラフ群を表に
        s = convertKeyValueParasToTable(s);

        // --- 非黑名單：來源語彙重疊度で無関文を排除 ---
        java.util.Set<String> dictForFilter = buildTermSet(itemName, s);
        s = filterParagraphsByOverlap(s, dictForFilter);

        // 取るに足らない短小セクション削除
        s = dropTrivialSections(s);
        // 隣接セクションの境界縮約
        s = collapseAdjacentSections(s);
        // 複数 body セクションを 1 つに統合
        s = mergeBodySections(s);

        s = s.trim();
        if (!s.startsWith("<section")) {
            return "<section class=\"desc-section body\"><p>" +
                    (itemName != null ? itemName + " の商品説明は登録されていません。" : "商品説明は登録されていません。") +
                    "</p></section>";
        }
        return s;
    }

    /** <p>Key：Val</p> 群を <table> に変換（医薬系キーに強めのハンドリング） */
    private static String convertKeyValueParasToTable(String html) {
        if (html == null || html.isBlank()) return html;

        java.util.regex.Pattern ptn = java.util.regex.Pattern.compile(
                "(?is)(<section[^>]*class=\"[^\"]*desc-section\\s*body[^\"]*\"[^>]*>)(.*?)(</section>)"
        );
        java.util.regex.Matcher m = ptn.matcher(html);
        StringBuffer out = new StringBuffer();

        while (m.find()) {
            String open = m.group(1);
            String inner = m.group(2);
            String close = m.group(3);

            // 全角/半角/互換コロンに対応
            java.util.regex.Pattern kv = java.util.regex.Pattern.compile(
                    "(?is)<p>\\s*([^：:︓﹕<]{1,30})\\s*[：:︓﹕]\\s*(.*?)\\s*</p>"
            );
            java.util.regex.Matcher km = kv.matcher(inner);

            java.util.List<String[]> rows = new java.util.ArrayList<>();
            while (km.find()) {
                rows.add(new String[]{ km.group(1).trim(), km.group(2).trim() });
            }

            final java.util.Set<String> medicalKeys = new java.util.HashSet<>(
                    java.util.Arrays.asList("効能・効果","効能","効果","用法・用量","用法","用量","成分","注意事項","お問い合わせ先","製造販売元","保管及び取扱い上の注意","保管及び取り扱い上の注意")
            );
            long hit = rows.stream().filter(r -> medicalKeys.contains(r[0])).count();
            boolean preferTable = (rows.size() >= 3) || (rows.size() >= 2 && hit >= 1);

            if (preferTable) {
                StringBuilder tbl = new StringBuilder();
                tbl.append("<section class=\"desc-section table\"><table>");
                for (String[] row : rows) {
                    tbl.append("<tr><th>").append(escapeHtml(row[0]))
                            .append("</th><td>").append(escapeHtml(row[1]))
                            .append("</td></tr>");
                }
                tbl.append("</table></section>");

                // body 内は表で置換（簡易化のため全置換）
                m.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(open + tbl + close));
            } else {
                m.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(m.group(0)));
            }
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** 取るに足らない <section> を除去 */
    private static String dropTrivialSections(String html) {
        if (html == null || html.isBlank()) return html;
        return html.replaceAll(
                "(?is)<section[^>]*>\\s*(?:<p>\\s*[。．、,\\-\\s]*</p>\\s*)*</section>", ""
        ).replaceAll(
                "(?is)<section[^>]*>\\s*(?:<p>\\s*</p>\\s*)+</section>", ""
        ).trim();
    }

    /** 隣接する section を詰める */
    private static String collapseAdjacentSections(String html) {
        if (html == null) return "";
        return html.replaceAll("(?is)</section>\\s+<section", "</section><section");
    }

    /** 複数 body セクションがある場合に 1 つへ統合 */
    private static String mergeBodySections(String html) {
        if (html == null || html.isBlank()) return html;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?is)<section[^>]*class=\"[^\"]*desc-section\\s*body[^\"]*\"[^>]*>(.*?)</section>");
        java.util.regex.Matcher m = p.matcher(html);
        StringBuilder mergedInner = new StringBuilder();
        int count = 0;
        while (m.find()) {
            if (count++ > 0) { /* 連結 */ }
            mergedInner.append(m.group(1));
        }
        if (count <= 1) return html;
        String withoutBodies = html.replaceAll("(?is)<section[^>]*class=\"[^\"]*desc-section\\s*body[^\"]*\"[^>]*>.*?</section>", "");
        return ("<section class=\"desc-section body\">" + mergedInner + "</section>") + withoutBodies;
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

    // ===== DICT（語彙）＆ 検収 =====

    /** 簡易語彙集合（原文から抽出） */
    private static java.util.Set<String> buildTermSet(String... sources) {
        java.util.Set<String> dict = new java.util.LinkedHashSet<>();
        if (sources != null) {
            for (String s : sources) {
                if (s == null) continue;
                String t = HtmlUtils.htmlUnescape(s).replace("\r","").replace("\n"," ");
                for (String w : t.split("[^\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}A-Za-z0-9%㎡㎖㎏\\-]+")) {
                    if (w == null) continue;
                    w = w.trim();
                    if (w.length() < 2) continue;
                    if (w.matches("[0-9]+")) continue;
                    dict.add(w);
                    if (dict.size() >= 300) break;
                }
            }
        }
        return dict;
    }

    /** 出力 <p> が DICT/数値と無関なら丸ごと削除（黒名單不要） */
    private static String filterParagraphsByOverlap(String html, java.util.Set<String> dict) {
        if (!StringUtils.hasText(html)) return html;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?is)(<p>)(.*?)(</p>)");
        java.util.regex.Matcher m = p.matcher(html);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String inner = m.group(2).replaceAll("\\s+", " ").trim();

            boolean hasNumberOrUnit = inner.matches(".*([0-9０-９]+\\s*(%|個|本|枚|g|kg|mL|ml|L|ℓ|cm|mm|㎜|㎝|㎖|㎡|m²|年|ヶ月|日)).*");
            int overlap = 0;
            for (String w : dict) {
                if (w.length() >= 2 && inner.contains(w)) {
                    overlap++;
                    if (overlap >= 2) break;
                }
            }
            if (overlap >= 2 || hasNumberOrUnit) {
                m.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(m.group(0)));
            } else {
                m.appendReplacement(out, "");
            }
        }
        m.appendTail(out);
        return out.toString();
    }
}
