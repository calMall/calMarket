package com.example.calmall.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// 注文履歴取得APIのレスポンスDTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListResponseDto {

    // レスポンスメッセージ（"success" または "fail"）
    private String message;

    // 注文履歴リスト
    private List<OrderSummary> orders;

    // 注文概要（1件）
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSummary {

        // 注文ID
        private Long orderId;

        // 商品コード
        private String itemCode;

        // 商品名
        private String itemName;

        // 商品価格
        private int price;

        // 数量
        private int quantity;

        // 日付（画面表示用）
        private String date;

        // 画像URLリスト
        private List<String> imageList;

        // 注文日時（秒まで）
        private String orderDate;
    }
}