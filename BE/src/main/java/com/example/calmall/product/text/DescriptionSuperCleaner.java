package com.example.calmall.product.text;

import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 楽天API向けの強化クリーナー。
 * - 入力（HTML/Plain/Caption）から最適なベースを選択
 * - ノイズ語・装飾・パンくず等を除去
 * - 「規格」「素材・成分」を賢く収集し1行に集約して<table>化
 * - 箇条書き(・/●/-/•/◆/■ 等)を<ul>化
 * - 断片的な短文は前後と連結し<p>化（切れ文対策）
 * - 出力は <section class="desc-section table|bullets|body"> のみ（見出しタグは生成しない）
 */
public final class DescriptionSuperCleaner {

    private DescriptionSuperCleaner() {}

    // ---------- 基本正規表現 ----------
    private static final Pattern TAG_PATTERN        = Pattern.compile("<[^>]+>");
    private static final Pattern BR_PATTERN         = Pattern.compile("(?i)<\\s*br\\s*/?>");
    private static final Pattern P_END_PATTERN      = Pattern.compile("(?i)</\\s*p\\s*>");
    private static final Pattern LI_END_PATTERN     = Pattern.compile("(?i)</\\s*li\\s*>");
    private static final Pattern TR_END_PATTERN     = Pattern.compile("(?i)</\\s*tr\\s*>");
    private static final Pattern SPACES_MANY        = Pattern.compile("[ \\t\\x0B\\f\\r　]+");
    private static final Pattern BULLET_HEAD        = Pattern.compile("^[\\p{Z}\\t　]*[・●\\-*•◆■□◇▶▷◉◦]\\s*");
    private static final Pattern KV_PATTERN         = Pattern.compile("^\\s*([^：:]{1,20})[：:]+\\s*(.+)$"); // keyは20字以内
    private static final Pattern SENTENCE_SPLIT_JA  = Pattern.compile("(?<=。|！|!|？|\\?|、)(?!$)");
    private static final Pattern REPEAT_SPEC        = Pattern.compile("(規格)+");
    private static final Pattern REPEAT_MATERIAL    = Pattern.compile("(素材・成分)+");

    // 量/サイズらしさ
    private static final Pattern SPEC_QUANT = Pattern.compile("(\\d+\\s*[枚包本個袋組箱]|\\d+\\s*(?:cm|mm|ml|mL|g|kg)|[SMXL]+|\\d+〜\\d+\\s*kg)");

    // パンくず等
    private static final Pattern BREADCRUMB = Pattern.compile("(.+?[＞>→].+)|(.+\\s*/\\s*.+)");

    // 連続装飾
    private static final Pattern DECOS = Pattern.compile("[★☆◆■●◎◇]+");

    // 連絡先/時間/電話
    private static final Pattern CONTACT = Pattern.compile("(?i)(受付時間|お問い合わせ|問合せ|お問合せ|カスタマー|support|営業時間|[0-2]?\\d[:：][0-5]\\d|\\d{2,4}-\\d{2,4}-\\d{3,4})");

    // URL/メール
    private static final Pattern URL_MAIL = Pattern.compile("(?i)(https?://\\S+|[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,})");

    // 記号だけ/数字だけ
    private static final Pattern ONLY_PUNCT_OR_NUM = Pattern.compile("^[\\p{Punct}\\s　0-9]+$");

    // ---------- ノイズ語 ----------
    private static final Set<String> JUNK_EXACT = setOf(
            "注意","特長","特長・注意","特徴","商品情報","商品説明","商品詳細","規格概要","仕様","スペック",
            "広告文責","販売元","製造元","メーカー","お問い合わせ","お問合せ","問合せ先","注意事項",
            "カテゴリ","カテゴリー","関連ワード","レビュー","レビュー募集中",
            "JANコード","JAN","型番","Product Code","product code",
            // 中
            "商品資訊","商品說明","產品資訊","規格","材質","品牌","注意事項"
    );
    private static final Set<String> JUNK_CONTAINS = setOf(
            "送料無料","SALE","セール","割引","ポイント","クーポン","ランキング",
            "在庫","納期","お届け","発送","メール便","宅配便","返品","交換","保証",
            "サイズ表は","画像はイメージ","予告なく","仕様は変更","モニター","カラーは",
            "メーカー希望小売価格","MSRP","お店TOP","TOPへ","カテゴリ","カテゴリー","＞"
    );

    // ---------- テーブルキー辞書 ----------
    private static final Set<String> SPEC_KEYS = setOf("規格","サイズ","寸法","容量","枚数","適応体重","重量","入数","対象年齢","数量","本体サイズ","枚数(適応体重)");
    private static final Set<String> MAT_KEYS  = setOf("素材","成分","材質","表面材","吸収材","防水材","止着材","伸縮材","結合材","原材料");

    // ---------- 公開API ----------
    public static String buildCleanHtml(String descriptionHtml, String descriptionPlain, String itemCaption) {
        // 1) ベース選択：Plain優先（HTMLはタグ除去＋改行保持）
        String base = selectBase(descriptionPlain, descriptionHtml, itemCaption);
        // 2) 正規化
        String normalized = normalize(base);
        // 3) 行へ分割（長大行のみ日本語句点で分割）
        List<String> lines = splitLines(normalized);

        // 収集器
        List<String> specBucket = new ArrayList<>();
        List<String> matBucket  = new ArrayList<>();
        List<String> bullets    = new ArrayList<>();
        List<String> paras      = new ArrayList<>();

        // 4) 行ごとに分類
        for (String raw : lines) {
            String t = squeeze(raw);
            if (t.isEmpty()) continue;
            if (isJunkLine(t)) continue;

            // 重複語の圧縮（規格規格規格 → 規格 など）
            t = REPEAT_SPEC.matcher(t).replaceAll("規格");
            t = REPEAT_MATERIAL.matcher(t).replaceAll("素材・成分");

            // 箇条書き
            if (BULLET_HEAD.matcher(t).find()) {
                String b = t.replaceFirst(BULLET_HEAD.pattern(), "").trim();
                if (!b.isEmpty()) bullets.add(b);
                continue;
            }

            // key:value（keyは20字以内に制限）
            Matcher kv = KV_PATTERN.matcher(t);
            if (kv.find()) {
                String key = kv.group(1).trim();
                String val = kv.group(2).trim();
                if (!key.isEmpty() && !val.isEmpty()) {
                    if (isMaterialKey(key)) {
                        matBucket.add(val);
                    } else if (isSpecKey(key) || SPEC_QUANT.matcher(val).find()) {
                        specBucket.add(val);
                    } else {
                        // その他のKVは段落として扱う（無闇に<th>を増やさない）
                        paras.add(key + "：" + val);
                    }
                }
                continue;
            }

            // 規格/数量らしさ
            if (containsAny(t, SPEC_KEYS) || SPEC_QUANT.matcher(t).find()) {
                specBucket.add(stripLeadingLabel(t, SPEC_KEYS, "規格"));
                continue;
            }

            // 素材・成分らしさ
            if (containsAny(t, MAT_KEYS)) {
                matBucket.add(stripLeadingLabel(t, MAT_KEYS, "素材・成分"));
                continue;
            }

            // それ以外は段落候補
            paras.add(t);
        }

        // 5) 断片短文の連結（見栄え最適化）
        paras = mergeTinyFragments(paras);

        // 6) テーブル行の整形（同種を1行へ集約）
        String specJoined = joinDistinct(specBucket, "、");
        String matJoined  = joinDistinct(matBucket,  "、");

        // 7) HTML構築
        StringBuilder out = new StringBuilder(512);

        if (!specJoined.isEmpty() || !matJoined.isEmpty()) {
            out.append("<section class=\"desc-section table\"><table>");
            if (!specJoined.isEmpty()) {
                out.append("<tr><th>")
                        .append(esc("規格"))
                        .append("</th><td>")
                        .append(esc(specJoined))
                        .append("</td></tr>");
            }
            if (!matJoined.isEmpty()) {
                out.append("<tr><th>")
                        .append(esc("素材・成分"))
                        .append("</th><td>")
                        .append(esc(matJoined))
                        .append("</td></tr>");
            }
            out.append("</table></section>");
        }

        if (!bullets.isEmpty()) {
            // 重複除去
            bullets = dedupeKeepOrder(bullets);
            out.append("<section class=\"desc-section bullets\"><ul>");
            for (String b : bullets) {
                out.append("<li>").append(esc(squeeze(b))).append("</li>");
            }
            out.append("</ul></section>");
        }

        if (!paras.isEmpty()) {
            out.append("<section class=\"desc-section body\">");
            for (String p : paras) {
                String x = squeeze(p);
                if (x.isEmpty() || isJunkLine(x)) continue;
                out.append("<p>").append(esc(x)).append("</p>");
            }
            out.append("</section>");
        }

        return out.toString();
    }

    /** 出力HTML → プレーンテキスト（改行保持、テスト用） */
    public static String toPlain(String html) {
        if (html == null) return "";
        String s = html;
        s = BR_PATTERN.matcher(s).replaceAll("\n");
        s = P_END_PATTERN.matcher(s).replaceAll("\n");
        s = LI_END_PATTERN.matcher(s).replaceAll("\n");
        s = TR_END_PATTERN.matcher(s).replaceAll("\n");
        s = TAG_PATTERN.matcher(s).replaceAll("");
        s = HtmlUtils.htmlUnescape(s);
        s = SPACES_MANY.matcher(s).replaceAll(" ");
        s = s.replaceAll("\\n{2,}", "\n");
        return s.trim();
    }

    // ---------- 内部ロジック ----------
    private static String selectBase(String plain, String html, String caption) {
        if (plain != null && !plain.isBlank()) return plain;
        if (html  != null && !html.isBlank())  return stripHtmlKeepBreaks(html);
        return caption == null ? "" : caption;
    }

    private static String stripHtmlKeepBreaks(String html) {
        String s = BR_PATTERN.matcher(html).replaceAll("\n");
        s = P_END_PATTERN.matcher(s).replaceAll("\n");
        s = LI_END_PATTERN.matcher(s).replaceAll("\n");
        s = TR_END_PATTERN.matcher(s).replaceAll("\n");
        return TAG_PATTERN.matcher(s).replaceAll("");
    }

    private static String normalize(String s) {
        String t = HtmlUtils.htmlUnescape(s == null ? "" : s);
        t = t.replace('\u00A0', ' ').replace('\u3000', ' ');
        t = DECOS.matcher(t).replaceAll("");
        t = t.replace("\r\n", "\n").replace("\r", "\n");
        t = SPACES_MANY.matcher(t).replaceAll(" ");
        return t.trim();
    }

    private static List<String> splitLines(String text) {
        List<String> out = new ArrayList<>();
        for (String raw : text.split("\\n")) {
            String t = raw.trim();
            if (t.isEmpty()) continue;
            // 長大行のみ句点で分割（過剰分割を避ける）
            if (t.length() > 220 && t.contains("。")) {
                for (String s : SENTENCE_SPLIT_JA.split(t)) {
                    String x = s.trim();
                    if (!x.isEmpty()) out.add(x);
                }
            } else {
                out.add(t);
            }
        }
        return out;
    }

    private static boolean isJunkLine(String s) {
        String t = s.trim();
        if (t.isEmpty()) return true;
        if (JUNK_EXACT.contains(t)) return true;
        if (BREADCRUMB.matcher(t).matches()) return true;
        if (CONTACT.matcher(t).find()) return true;
        if (URL_MAIL.matcher(t).find()) return true;
        if (ONLY_PUNCT_OR_NUM.matcher(t).matches()) return true;

        for (String kw : JUNK_CONTAINS) {
            if (t.contains(kw)) return true;
        }

        // 「モレ」「ムレ0へ！」など短いが意味がある語は残す。
        // ただし漢字/仮名/英数をほぼ含まない極短断片は除外
        if (t.length() <= 2 && !containsWordChar(t)) return true;

        return false;
    }

    private static boolean containsWordChar(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) return true;
            // ひらがな/カタカナ/漢字の範囲も簡易許容
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HIRAGANA) return true;
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.KATAKANA) return true;
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) return true;
        }
        return false;
    }

    private static boolean isSpecKey(String k) { return SPEC_KEYS.contains(k) || k.contains("サイズ") || k.contains("規格") || k.contains("枚数") || k.contains("容量"); }
    private static boolean isMaterialKey(String k){ return MAT_KEYS.contains(k)  || k.contains("素材")  || k.contains("成分")  || k.contains("材質"); }

    private static String stripLeadingLabel(String s, Set<String> labels, String fallback) {
        String out = s;
        for (String lb : labels) {
            if (out.startsWith(lb)) {
                out = out.substring(lb.length()).trim();
                if (out.startsWith("：") || out.startsWith(":")) out = out.substring(1).trim();
                break;
            }
        }
        // 全部消えて空になった場合は元語を返す
        return out.isEmpty() ? fallback : out;
    }

    private static String squeeze(String s) {
        if (s == null) return "";
        String t = SPACES_MANY.matcher(s).replaceAll(" ");
        t = t.replaceAll("\\s*（\\s*", "（").replaceAll("\\s*）\\s*", "）");
        t = t.replaceAll("\\s*\\(\\s*", "(").replaceAll("\\s*\\)\\s*", ")");
        t = t.replaceAll("\\s*×\\s*", "×").replaceAll("\\s*\\-\\s*", "-");
        return t.trim();
    }

    /** 文字列に指定語集合のいずれかが含まれているか */
    private static boolean containsAny(String s, Set<String> dict) {
        if (s == null || s.isEmpty()) return false;
        for (String k : dict) {
            if (s.contains(k)) return true;
        }
        return false;
    }

    private static List<String> mergeTinyFragments(List<String> paras) {

        if (paras.isEmpty()) return paras;
        List<String> out = new ArrayList<>();
        String carry = null;

        for (String p : paras) {
            String t = squeeze(p);
            if (t.length() <= 8) { // 短すぎる断片は次行へ合流
                carry = (carry == null) ? t : (carry + " " + t);
                continue;
            }
            if (carry != null) {
                out.add(squeeze(carry + " " + t));
                carry = null;
            } else {
                out.add(t);
            }
        }
        if (carry != null && !carry.isEmpty()) out.add(carry);
        return out;
    }

    private static String joinDistinct(List<String> list, String sep) {
        if (list == null || list.isEmpty()) return "";
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String s : list) {
            String t = squeeze(s);
            if (t.isEmpty()) continue;
            // 明らかなラベルの重複を除去
            t = REPEAT_SPEC.matcher(t).replaceAll("規格");
            t = REPEAT_MATERIAL.matcher(t).replaceAll("素材・成分");
            set.add(t);
        }
        return String.join(sep, set);
    }

    private static List<String> dedupeKeepOrder(List<String> items) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String s : items) {
            String t = squeeze(s);
            if (!t.isEmpty()) set.add(t);
        }
        return new ArrayList<>(set);
    }

    private static String esc(String s) {
        return HtmlUtils.htmlEscape(s == null ? "" : s);
    }

    private static Set<String> setOf(String... arr) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(arr)));
    }
}
