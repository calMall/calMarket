package com.example.calmall.orders.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class OrderCheckResponseDto {
    private boolean isAvailable; // 全ての商品が注文可能か
    private List<String> unavailableItems; // 注文不可能な商品のリスト
    private Integer totalPrice; // 合計金額
}