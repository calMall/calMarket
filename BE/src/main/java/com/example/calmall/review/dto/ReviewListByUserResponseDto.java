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

    // レスポンスメッセージ（success または fail）
    private String message;

    // ユーザーのレビュー一覧
    private List<UserReview> reviews;

    // 総ページ数（ページネーション用）
    private int totalPages;

    // 現在のページ番号（0始まり）
    private int currentPage;

    // 次のページが存在するか（true/false）
    private boolean hasNext;

    // 全レビュー件数
    private long totalElements;

    /**
     * ユーザーのレビュー情報（商品別）
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserReview {
        // レビューID
        private Long reviewId;

        // 商品コード（itemCode）
        private String itemCode;

        // 商品名
        private String itemName;

        // 商品画像（表示用に1枚目のみ使用）
        private String itemImage;

        // 評価（1〜5）
        private int rating;

        // レビュータイトル
        private String title;

        // レビュー本文
        private String comment;

        // 画像URLのリスト
        private List<String> imageList;

        // 投稿日時
        private String createdAt;
    }
}
