package com.example.calmall.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// レビュー投稿APIのリクエストDTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequestDto {

    // 商品コード
    private String itemCode;

    // ユーザーID
    private Long userId;

    // 評価スコア（1〜5）
    private int rating;

    // レビュータイトル
    private String title;

    // レビュー内容
    private String comment;

    // 画像URL
    private String image;
}