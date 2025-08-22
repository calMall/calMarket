package com.example.calmall.product.text;

/**
 * 入力テキストが全く無い商品のための簡易説明文を生成する。
 * - 画像点数と商品名のみを用いた保守的な文面
 * - 情報の断定や推測は行わない
 */
public final class DescriptionFallbackBuilder {

    private DescriptionFallbackBuilder(){}

    /**
     * メタ情報のみから説明HTMLを作成する。
     *
     * @param itemName   商品名（null可）
     * @param imageCount 画像の点数（0以上）
     * @return desc-section の本文セクション（HTML断片）
     */
    public static String buildFromMeta(String itemName, int imageCount) {
        String safeName = (itemName == null || itemName.isBlank()) ? "本商品" : "「" + esc(itemName.trim()) + "」";
        String countText = (imageCount > 0) ? "（画像" + imageCount + "点）" : "";
        // 文面はできるだけ中立・保守的にする
        String body = safeName
                + "の詳細なテキスト説明は現在未取得です。掲載されている商品画像"
                + countText
                + "をご確認のうえ、仕様や内容をご判断ください。正確な情報は、元の販売ページをご参照ください。";

        return "<section class=\"desc-section body\"><p>" + body + "</p></section>";
    }


    // 簡単なHTMLエスケープ
    private static String esc(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '&' -> sb.append("&amp;");
                case '"' -> sb.append("&quot;");
                case '\''-> sb.append("&#39;");
                default  -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
