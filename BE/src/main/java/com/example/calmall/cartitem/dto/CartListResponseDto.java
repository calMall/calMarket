package com.example.calmall.cartitem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * カート一覧取得APIのレスポンスDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartListResponseDto {

    private String message; // レスポンスメッセージ（"success" または "fail"）

    private List<CartItemDto> cartItems; // カート内の商品リスト

    /**
     * カート内の商品1件分の情報（APIレスポンス用）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemDto {

        private Long id;
        private String itemCode; // 商品コード
        private String itemName; // 商品名
        private int price;       // 価格
        private int quantity;    // 数量
        private List<String> imageUrls; // 画像URLリスト
        private String option; // 商品オプション (もし存在する場合)
    }
}