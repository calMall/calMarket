package com.example.calmall.orders.dto;

import lombok.Data;
import java.util.List;

@Data
public class FinalOrderRequestDto {
    private List<String> itemCodes;
    private String deliveryAddress;
}