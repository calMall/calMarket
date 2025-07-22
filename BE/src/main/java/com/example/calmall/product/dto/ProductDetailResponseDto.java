package com.example.calmall.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


/**
 * 商品詳細取得APIのレスポンスDTO
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponseDto {

    // レスポンスメッセージ（"success" または "fail"）
    private String message;

    // 商品詳細情報（下記のProductDtoを使用）
    private ProductDto product;

    // 商品情報を表すDTO（商品詳細の中身）
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDto {

        // 楽天APIの商品コード（itemCode）
        private String itemCode;

        // 商品名（itemName）
        private String itemName;

        // 商品の説明文（itemCaption）
        private String itemCaption;

        // キャッチコピー（catchcopy）
        private String catchcopy;

        // 平均スコア（レビュー評価）
        private int score;

        // レビュー件数
        private int reviewCount;

        // 商品価格
        private int price;

        // 商品画像URLリスト
        private List<String> imageUrls;
    }
}