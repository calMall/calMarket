package com.example.calmall.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * 払い戻しリクエスト用DTO
 */
@Getter
public class RefundRequestDto {

    @NotNull(message = "orderId は必須です")
    private Long orderId;
}
