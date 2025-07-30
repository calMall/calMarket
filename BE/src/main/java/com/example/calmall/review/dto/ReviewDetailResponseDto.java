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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Tokyo")
    private LocalDateTime createdAt; // 作成日時
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Tokyo")
    private LocalDateTime updatedAt; // 更新日時
}
