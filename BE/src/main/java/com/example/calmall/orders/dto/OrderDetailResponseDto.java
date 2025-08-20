package com.example.calmall.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponseDto {

    private String message;
    private OrderDetail order;

    // 注文詳細全体を表現するクラス
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDetail {
        private Long orderId;
        private String deliveryAddress;
        private LocalDateTime orderDate; // 注文日時をLocalDateTime型に
        private String status;
        private List<OrderItemDto> orderItems; // 注文商品をリストで保持
    }

    // 注文商品ごとの情報を表現するクラス
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDto {
        private String itemCode;
        private String itemName;
        private int price;
        private int quantity;
        private List<String> imageList;
    }
}