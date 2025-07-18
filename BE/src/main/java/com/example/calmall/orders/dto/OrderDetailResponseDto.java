package com.example.calmall.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// 注文詳細取得APIのレスポンスDTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponseDto {

    // レスポンスメッセージ（"success" または "fail"）
    private String message;

    // 注文情報
    private OrderDetail order;

    // 注文詳細の中身
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDetail {

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

        // 表示用日付
        private String date;

        // 画像リスト
        private List<String> imageList;

        // お届け先
        private String deliveryAddress;

        // 注文日時（秒まで）
        private String orderDate;
    }
}