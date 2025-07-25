package com.example.calmall.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * 商品詳細取得APIレスポンスDTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDetailResponseDto {

    /** "success" / "fail" */
    private String message;

    /** 商品詳細 */
    @JsonProperty("product")
    private ProductDto product;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDto {

        /** 楽天API itemCode */
        private String itemCode;

        /** 商品名 */
        private String itemName;

        /** 説明文 */
        private String itemCaption;

        /** キャッチコピー */
        private String catchcopy;

        /** レビュー平均（仮） */
        private int score;

        /** レビュー数（仮） */
        private int reviewCount;

        /** 価格 */
        private Integer price;

        /** 画像URL一覧 */
        private List<String> imageUrls;
    }
}
