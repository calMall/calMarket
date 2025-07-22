package com.example.calmall.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// 注文作成APIのリクエストDTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDto {

    // ユーザーID
    private Long userId;

    // 注文する商品リスト
    private List<OrderItem> items;

    // 配送先住所
    private String deliveryAddress;

    // オプション（例：ギフト包装）
    private String option;

    // 注文商品1件の情報
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {

        // 商品コード
        private String itemCode;

        // 数量
        private int quantity;
    }
}