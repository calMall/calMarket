// CartListForOrderResponseDto.java
package com.example.calmall.cartitem.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CartListForOrderResponseDto {
    private String message;
    private List<CartItemForOrderPageDto> cartList;
}