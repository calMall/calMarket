package com.example.calmall.review.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
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

    // レスポンスメッセージ（"success" または "fail"）
    private String message;

    // レビュー一覧
    private List<ReviewInfo> reviews;

    // 評価統計情報（null 許容）
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<RatingStat> ratingStats;

    // ログインユーザー自身のレビュー（null 許容）
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MyReview myReview;

    // 総ページ数（ページネーション用）
    private int totalPages;

    // 現在のページ番号（0始まり）
    private int currentPage;

    // 次ページが存在するかどうか
    private boolean hasNext;

    // 全レビュー件数
    private long totalElements;

    /**
     * レビュー情報を表す内部クラス
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewInfo {
        // レビューID
        private Long reviewId;

        // 投稿者のニックネーム
        private String userNickname;

        // 評価点数（1〜5）
        private int rating;

        // レビュータイトル
        private String title;

        // レビュー本文
        private String comment;

        // 画像URLのリスト（1枚以上）
        private List<String> imageList;

        // 投稿日時
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        // ログインユーザーがいいねしているか
        private boolean isLike;

        // このレビューの総いいね数
        private long likeCount;
    }

    /**
     * 各点数ごとのレビュー件数
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingStat {
        // 点数（例: 5, 4, 3, ...）
        private int score;

        // 件数（該当点数のレビュー数）
        private long count;
    }

    /**
     * ログインユーザー自身が投稿したレビュー
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyReview {
        // レビューID
        private Long reviewId;

        // 評価点数
        private int rating;

        // レビュータイトル
        private String title;

        // レビュー本文
        private String comment;

        // 画像URLのリスト
        private List<String> imageList;

        // ★ 投稿日時（JST）
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        // ★ このレビューの総いいね数
        private long likeCount;
    }
}
