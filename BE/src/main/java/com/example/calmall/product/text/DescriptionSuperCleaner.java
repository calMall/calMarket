package com.example.calmall.product.text;

import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 楽天APIの生テキスト/HTMLを「読みやすい安全なHTML断片」へ整形するユーティリティ。
 * - 外部ライブラリに依存せず、<p> / <ul><li> / <table><tr><th|td> のみ生成
 * - セクション見出し（h3等）は生成しない
 * - 大量のノイズ/装飾/JAN/問合せ情報/パンくず等を除去
 * - 「仕様/規格/サイズ/容量/素材・成分」等を優先的にテーブル化
 * - 箇条書きのマーカー（・●-＊• など）をUL化
 * - 残りは段落<p>として整形
 */
public final class DescriptionSuperCleaner {

    private DescriptionSuperCleaner() {}

    // =========================================================
    //  基本設定
    // =========================================================

    /** デバッグログの出力可否 */
    private static final boolean DEBUG = false;

    // =========================================================
    //  正規表現・定数
    // =========================================================

    /** HTMLタグ全般（除去用） */
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");

    /** 箇条書き先頭（全角/半角・各種記号に対応） */
    private static final Pattern BULLET_HEAD = Pattern.compile("^[\\p{Z}\\t　]*[・●\\-*•◆■□◇▶▷◉◦]\\s*");

    /** Key:Value 検出（「：」「:」のいずれも対応、キー長40まで） */
    private static final Pattern KV_PATTERN = Pattern.compile("^\\s*([^：:]{1,40})[：:]+\\s*(.+)$");

    /** 数量・サイズ・容量の典型パターン（例：62枚×4個 / 5〜10kg） */
    private static final Pattern SPEC_QUANT_PATTERN = Pattern.compile("(?i)(\\d+\\s*[枚包本個袋組箱]|[SMXL]+|\\d+\\s*(?:cm|mm|ml|mL|g|kg))");

    /** パンくず/カテゴリ行の典型（「＞」や「/」で区切られるナビ） */
    private static final Pattern BREADCRUMB_PATTERN = Pattern.compile("(.+?[＞>→].+)|(.+\\s*/\\s*.+)");

    /** 問合せ/営業時間/電話番号などの典型ノイズ */
    private static final Pattern CONTACT_PATTERN = Pattern.compile(
            "(?i)(受付時間|お問い合わせ|問合せ|お問合せ|カスタマー|support|営業時間|am|pm|[0-2]?\\d[:：][0-5]\\d|\\d{2,4}-\\d{2,4}-\\d{3,4})");

    /** JANコード/型番等の典型 */
    private static final Pattern JAN_MODEL_PATTERN = Pattern.compile("(?i)(JAN|JANコード|型番|product\\s*code|item\\s*code|型\\s*式)[：:\\s]*([A-Za-z0-9\\-_/]+)?");

    /** 装飾記号を連続で含むノイズ */
    private static final Pattern DECORATIVE_PATTERN = Pattern.compile("[★☆◆■●◎◇]+");

    /** URL/メールなど（説明に不要なケースが多い） */
    private static final Pattern URL_MAIL_PATTERN = Pattern.compile("(?i)(https?://\\S+|[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,})");

    /** 数字や記号だけの行（広告/区切りの可能性） */
    private static final Pattern BARE_SYMBOLS_PATTERN = Pattern.compile("^[\\p{Punct}\\s　0-9]+$");

    /** 半角/全角スペース・制御文字の圧縮用 */
    private static final Pattern SPACES_MANY = Pattern.compile("[ \\t\\x0B\\f\\r　]+");

    /** 句点・読点などで疑似分割（長文対策） */
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=。|！|!|？|\\?|\\.|、)(?!$)");

    // ------------------------------
    //  ノイズ語（完全一致・先頭一致・包含で判定）
    // ------------------------------

    /** 完全一致で捨てる語（見出し残骸/不要単語） */
    private static final Set<String> JUNK_WORDS_EXACT = setOf(
            // 和文
            "注意", "特長", "特長・注意", "特徴", "商品情報", "商品説明", "商品詳細", "規格概要", "仕様", "スペック",
            "広告文責", "販売元", "製造元", "メーカー", "お問い合わせ", "お問合せ", "問合せ先", "注意事項",
            "カテゴリ", "カテゴリー", "関連ワード",
            // 中文
            "注意事項", "商品資訊", "商品說明", "產品資訊", "規格", "材質", "品牌",
            // 英文
            "spec", "specs", "specification", "specifications", "details", "description", "info"
    );

    /** 先頭一致で捨てる語（「◎商品説明」「★規格概要」など） */
    private static final Set<String> JUNK_PREFIXES = setOf(
            "◎", "◇", "◆", "■", "●", "★", "☆", "※",
            "【注意】", "【注意事項】", "【商品情報】", "【商品説明】", "【商品詳細】", "【仕様】", "【スペック】",
            "《注意》", "《商品情報》", "《商品説明》", "《商品詳細》", "《仕様》", "《スペック》",
            "◆注意", "◆商品情報", "◆商品説明", "◆仕様", "◆スペック",
            "【JANコード】", "JANコード", "JAN:", "JAN：",
            "メーカー", "製造元", "販売元", "広告文責", "お問い合わせ", "問合せ先", "お問合せ"
    );

    /** 行に含まれていたら大抵不要（含むだけで除去） */
    private static final Set<String> JUNK_CONTAINS = setOf(
            "送料無料", "SALE", "セール", "割引", "ポイント", "クーポン", "ランキング", "レビュー募集中",
            "在庫", "納期", "お届け", "発送", "メール便", "宅配便",
            "返品", "交換", "保証", "注意喚起", "ご注意",
            "サイズ表は", "画像はイメージ", "予告なく", "仕様は変更", "モニター", "カラーは",
            "メーカー希望小売価格", "MSRP",
            "お店TOP", "カテゴリ", "カテゴリー", "＞", "TOPへ",
            "受付時間", "営業時間", "お問い合わせ", "問合せ"
    );

    /** 規格・サイズ・容量に関するキー語（テーブル化対象） */
    private static final Set<String> SPEC_KEYS = setOf(
            "規格", "規格概要", "サイズ", "寸法", "容量", "枚数", "適応体重", "重量", "入数", "対象年齢", "数量", "数量・容量", "本体サイズ",
            "Size", "SIZE", "weight", "Weight", "容量(約)", "枚数(適応体重)"
    );

    /** 素材・成分に関するキー語（テーブル化対象） */
    private static final Set<String> MATERIAL_KEYS = setOf(
            "素材", "成分", "材質", "表面材", "吸収材", "防水材", "止着材", "伸縮材", "結合材",
            "素材／成分", "素材/成分", "材質・成分", "Material", "Materials", "原材料", "主要材質"
    );

    /** 不要セパレータ／装飾列の候補 */
    private static final Set<String> PURE_SEPARATORS = setOf(
            "-", "–", "—", "――", "―――", "――――", "ーー", "——", "＊", "***", "*****", "======", "====", "----", "＿＿", "__", "___"
    );

    // =========================================================
    //  メインAPI
    // =========================================================

    /**
     * 入力のHTML/プレーンテキスト/キャプションから、最も妥当な基底テキストを選び、
     * 段階的にクレンジング・再整形し、最終的なHTMLを返す。
     * 生成する要素：<section class="desc-section body|bullets|table"> + <p>/<ul>/<table>
     */
    public static String buildCleanHtml(String descriptionHtml, String descriptionPlain, String itemCaption) {
        // 1) ベース文字列を決定（HTMLなら改行を保持したままタグ除去）
        String base = chooseBase(descriptionHtml, descriptionPlain, itemCaption);
        // 2) ノーマライズ（HTMLアンエスケープ、全角/半角空白調整、改行統一）
        String normalized = normalize(base);
        // 3) 大段落が極端に長い場合は簡易的に文区切り
        List<String> lines = splitToLinesWithFallback(normalized);

        if (DEBUG) {
            debug("BASE", base);
            debug("NORMALIZED", normalized);
            debug("LINES", String.join(" | ", lines));
        }

        // 4) 一次フィルタ：行ごとにノイズ判定をかけて除外
        List<String> filtered = new ArrayList<>();
        for (String ln : lines) {
            String t = ln.trim();
            if (t.isEmpty()) continue;
            if (isJunkLine(t)) continue;
            filtered.add(t);
        }

        // 5) 構造抽出：KV（spec/material優遇）、bullet、paragraph
        List<String[]> tableRows = new ArrayList<>(); // {key, value}
        List<String> bullets = new ArrayList<>();
        List<String> paragraphs = new ArrayList<>();

        for (String ln : filtered) {
            String t = ln.trim();

            // 5-1) 箇条書き
            if (BULLET_HEAD.matcher(t).find()) {
                bullets.add(t.replaceFirst(BULLET_HEAD.pattern(), "").trim());
                continue;
            }

            // 5-2) Key:Value
            Matcher mkv = KV_PATTERN.matcher(t);
            if (mkv.find()) {
                String key = mkv.group(1).trim();
                String value = mkv.group(2).trim();

                // 仕様/規格/素材系のキーはそのまま採用
                if (!key.isEmpty() && !value.isEmpty()) {
                    tableRows.add(new String[]{key, value});
                    continue;
                }
            }

            // 5-3) 「規格・枚数・サイズ」等のインラインパターンをテーブルへ補足的に寄せる
            if (containsAny(t, SPEC_KEYS) || SPEC_QUANT_PATTERN.matcher(t).find()) {
                // 典型的な「サイズ: 内容」「枚数(適応体重)....」のような行を粗く拾う
                // a) すでに「key:value」ならKVとして処理済み
                // b) ここではセミコロン/コロン/読点で粗く切ってテーブルへ
                boolean captured = false;
                if (t.contains("：") || t.contains(":")) {
                    String[] parts = t.split("[：:]", 2);
                    if (parts.length == 2) {
                        String k = parts[0].trim();
                        String v = parts[1].trim();
                        if (!k.isEmpty() && !v.isEmpty() && !k.equals(v)) {
                            tableRows.add(new String[]{k, v});
                            captured = true;
                        }
                    }
                }
                if (!captured) {
                    // 「Mはいはい 62枚×4個（5〜10kg）」のような行は「規格」キーに寄せる
                    tableRows.add(new String[]{"規格", t});
                }
                continue;
            }

            // 5-4) 素材・成分の可能性
            if (containsAny(t, MATERIAL_KEYS)) {
                if (t.contains("：") || t.contains(":")) {
                    String[] parts = t.split("[：:]", 2);
                    if (parts.length == 2) {
                        String k = parts[0].trim();
                        String v = parts[1].trim();
                        if (!k.isEmpty() && !v.isEmpty() && !k.equals(v)) {
                            tableRows.add(new String[]{k, v});
                            continue;
                        }
                    }
                }
                tableRows.add(new String[]{"素材・成分", t});
                continue;
            }

            // 5-5) その他は段落
            paragraphs.add(t);
        }

        // 6) HTML 構築（必ずエスケープの上でタグを生成）
        StringBuilder out = new StringBuilder(normalized.length() + 512);

        if (!bullets.isEmpty()) {
            out.append("<section class=\"desc-section bullets\"><ul>");
            for (String b : bullets) {
                String x = squeeze(b);
                if (x.isEmpty() || isJunkLine(x)) continue;
                out.append("<li>").append(esc(x)).append("</li>");
            }
            out.append("</ul></section>");
        }

        if (!tableRows.isEmpty()) {
            // SPEC/素材キーを上に寄せるために簡易ソート
            tableRows.sort((a, b) -> {
                boolean aSpec = isSpecLike(a[0]);
                boolean bSpec = isSpecLike(b[0]);
                if (aSpec != bSpec) return aSpec ? -1 : 1;
                boolean aMat = isMaterialLike(a[0]);
                boolean bMat = isMaterialLike(b[0]);
                if (aMat != bMat) return aMat ? -1 : 1;
                return a[0].compareTo(b[0]);
            });

            out.append("<section class=\"desc-section table\"><table>");
            for (String[] row : tableRows) {
                String k = squeeze(row[0]);
                String v = squeeze(row[1]);
                if (k.isEmpty() && v.isEmpty()) continue;
                out.append("<tr><th>").append(esc(k)).append("</th><td>").append(esc(v)).append("</td></tr>");
            }
            out.append("</table></section>");
        }

        if (!paragraphs.isEmpty()) {
            out.append("<section class=\"desc-section body\">");
            for (String p : paragraphs) {
                String x = squeeze(p);
                if (x.isEmpty() || isJunkLine(x)) continue;
                out.append("<p>").append(esc(x)).append("</p>");
            }
            out.append("</section>");
        }

        String html = clampToLastSection(out.toString());
        if (DEBUG) debug("HTML", html);
        return html;
    }

    /**
     * HTMLをプレーンテキストへ（改行保持）。内部利用やテストに。
     */
    public static String toPlain(String html) {
        if (html == null) return "";
        String s = html;
        s = s.replaceAll("(?i)<\\s*br\\s*/?>", "\n");
        s = s.replaceAll("(?i)</\\s*p\\s*>", "\n");
        s = s.replaceAll("(?i)</\\s*li\\s*>", "\n");
        s = s.replaceAll("(?i)</\\s*tr\\s*>", "\n");
        s = TAG_PATTERN.matcher(s).replaceAll("");
        s = HtmlUtils.htmlUnescape(s);
        s = s.replace("\u00A0", " ");
        s = SPACES_MANY.matcher(s).replaceAll(" ");
        s = s.replaceAll("\\n{2,}", "\n").trim();
        return s;
    }

    // =========================================================
    //  内部ヘルパー
    // =========================================================

    /** HTML/プレーン/キャプションからベース文字列を選択（HTMLは改行保持でタグ除去） */
    private static String chooseBase(String html, String plain, String caption) {
        if (html != null && !html.isBlank())  return stripHtmlKeepBreaks(html);
        if (plain != null && !plain.isBlank()) return plain;
        return caption != null ? caption : "";
    }

    /** HTMLのまま改行相当を\nへ寄せ、他タグを除去して返す */
    private static String stripHtmlKeepBreaks(String html) {
        String s = html;
        s = s.replaceAll("(?i)<\\s*br\\s*/?>", "\n");
        s = s.replaceAll("(?i)</\\s*p\\s*>", "\n");
        s = s.replaceAll("(?i)</\\s*li\\s*>", "\n");
        s = s.replaceAll("(?i)</\\s*tr\\s*>", "\n");
        s = TAG_PATTERN.matcher(s).replaceAll("");
        return s;
    }

    /** アンエスケープ、空白正規化、改行正規化 */
    private static String normalize(String s) {
        if (s == null) return "";
        String t = HtmlUtils.htmlUnescape(s);
        t = t.replace('\u00A0', ' ').replace('\u3000', ' ');
        t = t.replace("\r\n", "\n").replace("\r", "\n");
        // 装飾の連打を簡易除去
        t = DECORATIVE_PATTERN.matcher(t).replaceAll("");
        // コロンの全角半角を統一
        t = t.replace('：', '：'); // 置き換えの意図可読性用（将来拡張時のフック）
        return t.trim();
    }

    /** 長文対策：各行が極端に長ければ句読点で分割してからライン化 */
    private static List<String> splitToLinesWithFallback(String text) {
        List<String> lines = new ArrayList<>();
        String[] raw = text.split("\\n");
        for (String r : raw) {
            String t = r.trim();
            if (t.isEmpty()) continue;
            if (t.length() > 120 && t.indexOf('。') >= 0) {
                // 日本語の句点を優先して分割
                for (String s : SENTENCE_SPLIT.split(t)) {
                    String x = s.trim();
                    if (!x.isEmpty()) lines.add(x);
                }
            } else {
                lines.add(t);
            }
        }
        return lines;
    }

    /** 文字列の不要空白を圧縮し、前後をtrim */
    private static String squeeze(String s) {
        if (s == null) return "";
        String t = SPACES_MANY.matcher(s).replaceAll(" ");
        t = t.replaceAll("\\s*（\\s*", "（").replaceAll("\\s*）\\s*", "）");
        t = t.replaceAll("\\s*\\(\\s*", "(").replaceAll("\\s*\\)\\s*", ")");
        t = t.replaceAll("\\s*×\\s*", "×").replaceAll("\\s*\\-\\s*", "-");
        return t.trim();
    }

    /** 行がノイズかどうか主観判定 */
    private static boolean isJunkLine(String t) {
        String s = t.trim();

        // 完全一致の不要語
        if (JUNK_WORDS_EXACT.contains(s)) return true;

        // セパレータのみ
        if (PURE_SEPARATORS.contains(s)) return true;

        // パンくずやカテゴリっぽい
        if (BREADCRUMB_PATTERN.matcher(s).matches()) return true;

        // 問合せ/営業時間/電話など
        if (CONTACT_PATTERN.matcher(s).find()) return true;

        // JAN/型番の羅列
        if (JAN_MODEL_PATTERN.matcher(s).find() && s.length() <= 40) return true;

        // URL/メール
        if (URL_MAIL_PATTERN.matcher(s).find()) return true;

        // 装飾記号だけ/数字記号だけ
        if (BARE_SYMBOLS_PATTERN.matcher(s).matches()) return true;

        // 先頭一致で捨てる
        for (String pre : JUNK_PREFIXES) {
            if (s.startsWith(pre)) return true;
        }

        // 含むだけで捨てる
        for (String kw : JUNK_CONTAINS) {
            if (s.contains(kw)) return true;
        }

        // 「モレ」「ムレ0へ！」のような短い断片（一般に説明として弱い）
        if (s.length() <= 2 && !isAlnum(s)) return true;

        return false;
    }

    /** すべて英数字記号だけかどうか（短断片の判定補助） */
    private static boolean isAlnum(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c)) continue;
            if ("-_./:+".indexOf(c) >= 0) continue;
            return false;
        }
        return true;
    }

    /** 任意の集合に含まれる語が含有されるか */
    private static boolean containsAny(String s, Set<String> dict) {
        for (String k : dict) {
            if (s.contains(k)) return true;
        }
        return false;
    }

    /** テーブル上で仕様に近いキーか */
    private static boolean isSpecLike(String key) {
        return SPEC_KEYS.contains(key) || key.contains("サイズ") || key.contains("規格") || key.contains("枚数") || key.contains("容量");
    }

    /** テーブル上で素材に近いキーか */
    private static boolean isMaterialLike(String key) {
        return MATERIAL_KEYS.contains(key) || key.contains("素材") || key.contains("成分") || key.contains("材質");
    }

    /** 最後の</section>までで切り詰め（未閉じ対策） */
    private static String clampToLastSection(String html) {
        if (html == null) return null;
        int i = html.lastIndexOf("</section>");
        return (i >= 0) ? html.substring(0, i + "</section>".length()) : html;
        // セクション未出力の場合はそのまま
    }

    /** 最小限のHTMLエスケープ（高速） */
    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '&' -> sb.append("&amp;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /** 不変セット生成（読みやすさ重視） */
    private static Set<String> setOf(String... arr) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(arr)));
    }

    // =========================================================
    //  デバッグ用
    // =========================================================
    private static void debug(String tag, String msg) {
        if (DEBUG) System.out.println("[DESC] " + tag + " :: " + msg);
    }
}
