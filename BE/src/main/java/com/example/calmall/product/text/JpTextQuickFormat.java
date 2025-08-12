package com.example.calmall.product.text;

// 日本語テキストの簡易整形
public final class JpTextQuickFormat {
    private JpTextQuickFormat() {}
    private static String nz(String s){ return s==null ? "" : s; }

    public static String toReadablePlain(String raw){
        String t = nz(raw)
                .replace('\u3000', ' ')                 // 全角空白→半角
                .replaceAll("\\s{2,}", " ")            // 多重空白圧縮
                .replaceAll("[◆■●○◎▲▼◆■]+\\s*", "・") // 記号→中黒
                .replaceAll("(・\\s*){2,}", "・");      // 連続・圧縮
        t = t.replaceAll("(?<=[。！？])\\s*(?=\\S)", "\n"); // 句点等の後で改行
        t = t.replaceAll("、\\s*", "、\n");                // 読点後も軽く改行
        return t.trim();
    }
}
