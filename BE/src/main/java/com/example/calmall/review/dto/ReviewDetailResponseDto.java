package com.example.calmall.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * レビュー詳細取得のレスポンスDTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDetailResponseDto {
    private String title; // レビュータイトル
    private String comment; // コメント
    private int rating; // 評価（1〜5）
    private List<String> imageList; // 画像URLのリスト
    private LocalDateTime createdAt; // 作成日時
    private LocalDateTime updatedAt; // 更新日時
}
