package com.example.calmall.cartitem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// カート一覧取得APIのレスポンスDTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartListResponseDto {

    // レスポンスメッセージ（"success" または "fail"）
    private String message;

    // カート内の商品リスト
    private List<CartItem> cartItems;

    // カート内の商品1件分の情報
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItem {

        // 商品コード
        private String itemCode;

        // 商品名
        private String itemName;

        // 価格
        private int price;

        // 数量
        private int quantity;

        // 画像URLリスト
        private List<String> imageUrls;
    }
}