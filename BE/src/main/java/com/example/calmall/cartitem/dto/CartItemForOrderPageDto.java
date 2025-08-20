package com.example.calmall.cartitem.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartItemForOrderPageDto {
    private Long id;
    private String itemCode;
    private String itemName;
    private Integer price;
    private Integer quantity;
    private String imageUrl; // 1枚目の画像URL
}