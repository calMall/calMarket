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

    // レスポンスメッセージ（"success" または "fail"）
    private String message;

    // レビュー一覧
    private List<UserReview> reviews;

    /**
     * ユーザーが投稿したレビュー情報（1レビュー単位）
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserReview {
        // レビューID
        private Long reviewId;

        // 商品コード（楽天APIのitemCode）
        private String itemCode;

        // 商品名
        private String itemName;

        // 商品のメイン画像URL
        private String itemImage;

        // レビューの評価点（1〜5）
        private int rating;

        // レビュータイトル（任意）
        private String title;

        // レビュー本文
        private String comment;

        // 🔽 修正：画像リストに対応（複数画像対応に拡張）
        private List<String> imageList;

        // 投稿日時（文字列形式）
        private String createdAt;
    }
}
