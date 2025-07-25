package com.example.calmall.review.dto;

import lombok.*;

import java.util.List;

/**
 * ユーザーごとのレビュー取得APIのレスポンスDTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewListByUserResponseDto {

    // レスポンスメッセージ（例: success / fail）
    private String message;

    // レビュー一覧
    private List<UserReview> reviews;

    /**
     * ユーザー自身のレビュー情報
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserReview {
        private Long reviewId;
        private String itemCode;
        private String itemName;
        private String itemImage;  // メイン画像URL
        private int rating;
        private String title;
        private String comment;
        private String image;
        private String createdAt;
    }
}
