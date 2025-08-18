package com.example.calmall.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

/**
 * レビュー投稿用のリクエストDTOクラス
 */
@Getter
public class ReviewRequestDto {

    // 商品コード
    @NotBlank(message = "itemCode は必須です")
    private String itemCode;

    // 評価点（1〜5の整数）
    @Min(value = 1, message = "rating は1以上である必要があります")
    @Max(value = 5, message = "rating は5以下である必要があります")
    private int rating;

    // レビュータイトル（任意）
    private String title;

    // レビュー本文（空欄不可）
    @NotBlank(message = "comment は必須です")
    private String comment;

    // 画像URLのリスト（任意）
    private List<String> imageList;
}
