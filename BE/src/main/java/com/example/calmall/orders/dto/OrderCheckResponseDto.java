package com.example.calmall.orders.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class OrderCheckResponseDto {
    private String message;
    private Long totalPrice;
    private Map<String, Integer> insufficientItems; // 在庫不足の商品リスト
}