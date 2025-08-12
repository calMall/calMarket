package com.example.calmall.product.text;

import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 日本語の長文（句点や読点が欠落/過剰な空白がある説明文）を、可読なプレーンテキストに整形するユーティリティ
public final class JpTextQuickFormat {

    // 改行候補に使う句読点や区切り文字
    private static final String BREAK_CHARS = "[。．！!？?；;：:]";
    // 箇条書き先頭の検出（・や-、●、■など）
    private static final Pattern BULLET_HEAD = Pattern.compile("^[\\p{Z}\\t　]*[・\\-\\—\\●\\■\\□\\*]\\s*");

    // 「キー：値」形式の検出（全角/半角コロン対応）
    private static final Pattern KEY_VALUE = Pattern.compile("^\\s*([^：:]{1,30})[：:][\\s　]*(.+)$");

    // よく出る項目名（前方一致で判定し表として見やすくするためのスコア付け）
    private static final Set<String> KNOWN_KEYS = new HashSet<>(Arrays.asList(
            "製品名","一般的名称","販売名","原材料","材質","レンズ直径","DIA","ベースカーブ","BC","含水率",
            "製法","製造方法","着色方法","レンズタイプ","度数","PWR","製造国","原産国","医療機器承認番号",
            "販売業者","製造販売業者名","装用期間","使用期限","管理医療機器","注意","使用上の注意","区分","分類"
    ));

    private JpTextQuickFormat() {}

    // プレーンテキスト化のエントリポイント
    public static String toReadablePlain(String raw) {
        if (!StringUtils.hasText(raw)) return "";

        // 正規化（NFKC）で全角/半角のゆらぎを軽減し、改行/空白を整える
        String s = Normalizer.normalize(raw, Normalizer.Form.NFKC);
        s = s.replace("\r\n", "\n").replace("\r", "\n");
        s = htmlEntityQuickUnescape(s);
        s = collapseSpaces(s);

        // 粗く分割：改行が無い場合でも、句点などで擬似改行
        List<String> lines = smartSplitToLines(s);

        // 「キー：値」行と箇条書き行を見やすくまとめる
        List<String> out = new ArrayList<>();
        List<String> table = new ArrayList<>();
        List<String> bullets = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            Matcher kv = KEY_VALUE.matcher(trimmed);
            if (kv.find() && looksLikeKey(kv.group(1))) {
                table.add(kv.group(1).trim() + "： " + kv.group(2).trim());
                continue;
            }

            if (BULLET_HEAD.matcher(trimmed).find()) {
                // 箇条書きへ
                bullets.add(trimmed.replaceFirst(BULLET_HEAD.pattern(), "").trim());
                continue;
            }

            // どれでもなければ本文として改行付きで追加
            out.add(trimmed);
        }

        // 表パート
        if (!table.isEmpty()) {
            out.add(0, joinWithNewline(table)); // 先頭に集約
        }
        // 箇条書きパート
        if (!bullets.isEmpty()) {
            out.add("・" + String.join("\n・", bullets));
        }

        // 最終整形：不要な連続改行を圧縮
        String joined = joinWithNewline(out);
        joined = joined.replaceAll("\\n{3,}", "\n\n");
        return joined.trim();
    }

    // ざっくりHTMLエンティティを戻す（頻出のみ・高速）
    private static String htmlEntityQuickUnescape(String s) {
        return s.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    // 余分な空白（全角/半角）を1つへ圧縮
    private static String collapseSpaces(String s) {
        // 全角スペースも対象に
        s = s.replace('\u00A0', ' '); // NBSP
        s = s.replace('\u3000', ' '); // 全角スペース
        return s.replaceAll("[ \\t]{2,}", " ").trim();
    }

    // 句点・疑問符などで適度に改行。すでに改行があれば尊重
    private static List<String> smartSplitToLines(String s) {
        if (s.contains("\n")) {
            // 既存の改行をベースにし、さらに長すぎる行は句点で分割
            List<String> base = new ArrayList<>();
            for (String ln : s.split("\\n")) {
                base.addAll(splitLongByPunct(ln, 60)); // 目安60文字で折り返し
            }
            return base;
        }
        return splitLongByPunct(s, 60);
    }

    // 指定長を超える行を句読点で分割
    private static List<String> splitLongByPunct(String line, int max) {
        List<String> res = new ArrayList<>();
        String work = line.trim();
        if (work.length() <= max) {
            res.add(work);
            return res;
        }
        // 句読点でいったん区切る
        String[] chunks = work.split("(?<=" + BREAK_CHARS + ")");
        StringBuilder buf = new StringBuilder();
        for (String c : chunks) {
            if (buf.length() + c.length() > max && buf.length() > 0) {
                res.add(buf.toString().trim());
                buf.setLength(0);
            }
            buf.append(c);
        }
        if (buf.length() > 0) res.add(buf.toString().trim());
        return res;
    }

    // 「キー」に見えるかの簡易判定（既知キーなら強めに採用）
    private static boolean looksLikeKey(String key) {
        String k = key.trim();
        if (KNOWN_KEYS.stream().anyMatch(k::startsWith)) return true;
        // 末尾が「名」「率」「法」「番号」「国」「値」「分類」などもキーっぽい
        return k.matches(".*(名|率|法|番号|国|値|分類|期間|方法|直径|曲|数)$");
    }

    private static String joinWithNewline(List<String> list) {
        return String.join("\n", list);
    }
}
