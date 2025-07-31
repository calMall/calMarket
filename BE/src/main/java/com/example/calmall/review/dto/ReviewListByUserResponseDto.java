package com.example.calmall.review.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
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
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Tokyo")
        private LocalDateTime createdAt;

        // ログインユーザーがいいねしているか
        @JsonProperty("isLike")
        private boolean isLike;

        // このレビューの総いいね数
        private long likeCount;

        // ★ レビューの所有者かどうか
        @JsonProperty("isOwner")
        private boolean isOwner;
    }
}
