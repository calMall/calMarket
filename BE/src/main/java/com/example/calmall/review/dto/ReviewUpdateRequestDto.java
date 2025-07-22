package com.example.calmall.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// レビュー編集APIのリクエストDTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewUpdateRequestDto {

    // タイトル
    private String title;

    // 本文
    private String comment;

    // 画像URL
    private String image;
}
