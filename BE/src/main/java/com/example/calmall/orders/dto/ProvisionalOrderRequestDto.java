package com.example.calmall.orders.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProvisionalOrderRequestDto {
    private List<String> itemCodes; // 選択した商品のitemCodeのリスト
}