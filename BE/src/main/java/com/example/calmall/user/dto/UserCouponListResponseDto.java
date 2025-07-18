package com.example.calmall.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// クーポン一覧取得APIのレスポンスDTO(余裕があれば実装)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponListResponseDto {

    // 処理結果（"success" または "fail"）
    private String message;

    // 保有クーポンのリスト
    private List<Coupon> coupons;

    // クーポン情報
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coupon {
        private Long id;
        private String name;
        private int discount;
    }
}
