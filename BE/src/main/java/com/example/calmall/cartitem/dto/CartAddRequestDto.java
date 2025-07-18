package com.example.calmall.cartitem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// カートに商品を追加するリクエストDTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartAddRequestDto {

    // 商品コード
    private String itemCode;

    // 数量
    private int quantity;
}