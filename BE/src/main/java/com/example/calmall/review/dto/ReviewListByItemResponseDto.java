package com.example.calmall.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// 商品に対するレビュー一覧取得APIのレスポンスDTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewListByItemResponseDto {

    // レスポンスメッセージ
    private String message;

    // レビュー一覧
    private List<Review> reviews;

    // 点数別の人数
    private List<RatingStat> ratingStats;

    // 自分のレビュー
    private MyReview myReview;

    // レビュー1件の情報
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Review {

        private Long reviewId;
        private String userNickname;
        private int rating;
        private String title;
        private String comment;
        private List<String> imageList;
        private String createdAt;
        private boolean isLike;
        private int likeCount;
    }

    // 点数ごとの集計（例：score=5, count=2）
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingStat {
        private int score;
        private int count;
    }

    // 自分のレビュー（編集可能）
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyReview {
        private Long reviewId;
        private String title;
        private String comment;
        private int rating;
        private List<String> imageList;
    }
}
