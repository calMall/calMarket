//package com.example.calmall.product.text;
//
//import org.springframework.web.util.HtmlUtils;
//
///**
// * ローカル側の最小整形ユーティリティ。
// * ※ フォールバックは Facade/Formatter では使わない方針だが、
// *    toPlain 等の共通関数のために残している。
// */
//public final class DescriptionSuperCleanerBase {
//
//    private DescriptionSuperCleanerBase(){}
//
//    /** HTML があれば優先（改行保持してタグ除去）。無ければプレーン、最後にキャプション。 */
//    public static String chooseBasePreferHtml(String html, String plain, String caption) {
//        if (html != null && !html.isBlank())  return stripHtmlKeepBreaks(html);
//        if (plain != null && !plain.isBlank()) return plain;
//        return caption != null ? caption : "";
//    }
//
//    /** HTMLエンティティの実体化・全角/半角空白整理・改行正規化 */
//    public static String normalize(String s) {
//        if (s == null) return "";
//        String t = HtmlUtils.htmlUnescape(s);
//        t = t.replace('\u00A0', ' ').replace('\u3000',' ');
//        t = t.replace("\r\n","\n").replace("\r","\n").trim();
//        return t;
//    }
//
//    /** <br> や </p> を改行に置換してからタグ除去 */
//    public static String stripHtmlKeepBreaks(String html) {
//        String s = html;
//        s = s.replaceAll("(?i)<\\s*br\\s*/?>","\n");
//        s = s.replaceAll("(?i)</\\s*p\\s*>","\n");
//        s = s.replaceAll("(?i)</\\s*li\\s*>","\n");
//        s = s.replaceAll("(?i)</\\s*tr\\s*>","\n");
//        s = s.replaceAll("<[^>]+>","");
//        return s;
//    }
//
//    /** プレーンテキスト化（必要に応じて公開） */
//    public static String toPlain(String html) {
//        if (html == null) return "";
//        return stripHtmlKeepBreaks(html);
//    }
//
//    /** どうしても LLM も使えない時の簡易出力（※現在は使用しない設計） */
//    public static String localCleanToHtml(String descriptionHtml, String descriptionPlain, String itemCaption) {
//        String base = chooseBasePreferHtml(descriptionHtml, descriptionPlain, itemCaption);
//        if (base.isBlank()) return "";
//        return "<section class=\"desc-section body\"><p>" +
//                HtmlUtils.htmlEscape(base) +
//                "</p></section>";
//    }
//}
