package com.example.calmall.review.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * レビュー詳細取得のレスポンスDTO
 * - 投稿者の userId と ログインユーザー本人かどうかを示す isOwner を追加
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDetailResponseDto {

    // 投稿者のユーザーID（UUID形式）
    private String userId;

    private String title; // レビュータイトル
    private String comment; // コメント
    private int rating; // 評価（1〜5）
    private List<String> imageList; // 画像URLのリスト

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Tokyo")
    private LocalDateTime createdAt; // 作成日時

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Tokyo")
    private LocalDateTime updatedAt; // 更新日時

    private boolean like; // いいね済みか
    private long likeCount; // いいね数

    // ログインユーザーがこのレビューの投稿者であるかどうか
    private boolean isOwner;
}
