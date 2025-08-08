package com.example.calmall.orders.dto;

<<<<<<< HEAD
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class OrderCheckResponseDto {
    private boolean isAvailable; // 全ての商品が注文可能か
    private List<String> unavailableItems; // 注文不可能な商品のリスト
    private Integer totalPrice; // 合計金額
=======
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class OrderCheckResponseDto {
    private String message;
    private Long totalPrice;
    private Map<String, Integer> insufficientItems; // 在庫不足の商品リスト
>>>>>>> 38d7c7ae6928b4662b57f8a7985d1fec4ef8c16c
}