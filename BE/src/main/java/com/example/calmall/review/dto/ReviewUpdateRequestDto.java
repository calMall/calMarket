package com.example.calmall.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * レビュー更新用リクエストDTOクラス
 * ユーザーが自分の投稿済みレビューを編集する際に使用する
 */
@Getter
public class ReviewUpdateRequestDto {

    // 評価（1〜5の範囲で必須）
    @Min(value = 1, message = "rating は1以上である必要があります")
    @Max(value = 5, message = "rating は5以下である必要があります")
    private int rating;

    // 編集後のレビュータイトル（任意）
    private String title;

    // 編集後のレビュー本文（必須）
    @NotBlank(message = "comment は必須です")
    private String comment;

    // 編集後の画像URL（空文字可）
    private String image;
}
