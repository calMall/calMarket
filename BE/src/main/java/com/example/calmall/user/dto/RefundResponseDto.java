package com.example.calmall.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

/**
 * 払い戻しレスポンス用DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class RefundResponseDto {

    private String message;
    private List<CouponDto> coupons;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CouponDto {
        private Long id;         // クーポンID
        private String name;     // クーポン名
        private int discount;    // 割引金額（または割引率）
    }
}
