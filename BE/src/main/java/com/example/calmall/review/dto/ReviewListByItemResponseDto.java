package com.example.calmall.review.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * 商品ごとのレビュー取得APIのレスポンスDTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewListByItemResponseDto {

    // レスポンスメッセージ（例: success / fail）
    private String message;

    // レビュー一覧
    private List<ReviewInfo> reviews;

    // 統計情報（任意、null許容）
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RatingStat stats;

    // 自分のレビュー（任意、null許容）
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MyReview myReview;

    /**
     * 商品ごとのレビュー詳細情報
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewInfo {
        private Long reviewId;
        private String userNickname;
        private int rating;
        private String title;
        private String comment;
        private String image;
        private String createdAt;
        private boolean liked;
        private long likeCount;
    }

    /**
     * 商品ごとのレビュー評価統計情報
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingStat {
        private double average;  // 平均点
        private long count;      // 総レビュー数
    }

    /**
     * ログインユーザーの自身のレビュー
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyReview {
        private int rating;
        private String title;
        private String comment;
        private String image;
        private String createdAt;
    }
}
