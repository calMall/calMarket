package com.example.calmall.orders.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequestDto {
    private String userId;
    private List<OrderItemDto> items;
    private String deliveryAddress;

    @Data
    public static class OrderItemDto {
        private String itemCode;
        private Integer quantity;
    }
}