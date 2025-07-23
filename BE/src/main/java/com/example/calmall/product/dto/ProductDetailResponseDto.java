package com.example.calmall.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * 商品詳細取得API（GET /api/products/{itemCode}）のレスポンスDTO。
 * 仕様：
 * message: "success" または "fail"
 * 商品: 商品詳細データ（null可）
 *
 * ※「商品」キー名は仕様に合わせた日本語表記。内部フィールド名は product。
 * ※ itemName / itemCaption / catchcopy のキー名は仕様上変更可能だが、ここでは楽天API準拠のまま使用。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDetailResponseDto {

    /** レスポンスメッセージ（"success" / "fail"） */
    private String message;

    /** 商品詳細（キー名を日本語「商品」にする） */
    @JsonProperty("商品")
    private ProductDto product;

    /**
     * 商品詳細データ本体。
     * 楽天APIの代表カラムをマッピング。
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDto {

        /** 楽天APIの商品コード（例：jpntnc:10002379） */
        private String itemCode;

        /** 商品名 */
        private String itemName;

        /** 商品説明文 */
        private String itemCaption;

        /** キャッチコピー */
        private String catchcopy;

        /** レビュー平均点（現状は仮値） */
        private int score;

        /** レビュー件数（現状は仮値） */
        private int reviewCount;

        /** 商品価格 */
        private Integer price;

        /** 商品画像URLリスト */
        private List<String> imageUrls;
    }
}
