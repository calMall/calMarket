package com.example.calmall.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// ユーザーが投稿したレビュー一覧取得APIのレスポンスDTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewListByUserResponseDto {

    // レスポンスメッセージ
    private String message;

    // レビューリスト
    private List<Review> reviews;

    // レビュー1件の情報
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Review {
        private Long reviewId;
        private String itemCode;
        private String itemName;
        private int rating;
        private String title;
        private String comment;
        private String image;
        private String createdAt;
    }
}
