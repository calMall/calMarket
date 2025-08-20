package com.example.calmall.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ユーザー詳細取得APIのレスポンスDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailResponseDto {

    // 処理結果
    private String message;

    // ユーザー所持ポイント
    private int point;

    // 表示用の住所
    private List<String> deliveryAddresses;

    // 住所詳細リスト
    private List<AddressDetail> deliveryAddressDetails;

    // 注文履歴（最新10件まで）
    // 商品の画像と注文IDのみを簡易表示
    private List<OrderSummary> orders;

    // レビュー履歴（最新10件まで）
    // 投稿したレビューの概要を表示
    private List<ReviewSummary> reviews;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDetail {
        // 郵便番号
        private String postalCode;
        // 住所1
        private String address1;
        // 住所2
        private String address2;
    }

    // 注文履歴の1件分を表すDTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSummary {

        // 注文ID
        private Long id;

        // 商品の代表画像URL
        private String imageUrl;
    }

    // レビュー履歴の1件分を表すDTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewSummary {

        // レビューID
        private Long id;

        // レビュータイトル
        private String title;

        // レビュー投稿日時
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Tokyo")
        private LocalDateTime createdAt;

        // 評価スコア（1〜5）
        private int score;

        // 本文コメント
        private String content;

        // 配送先住所リスト
        private List<String> deliveryAddresses;
    }
}
