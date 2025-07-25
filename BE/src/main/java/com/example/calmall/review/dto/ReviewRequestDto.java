package com.example.calmall.review.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;

/**
 * レビュー投稿用リクエストDTOクラス（セッションからuser取得のためuserId削除）
 */
@Getter
public class ReviewRequestDto {

    // 商品のコード（楽天APIの商品識別子）フロントが埋め込み
    @NotBlank(message = "itemCode は必須です")
    private String itemCode;

    // レビュー評価（1〜5）
    @Min(value = 1, message = "rating は1以上である必要があります")
    @Max(value = 5, message = "rating は5以下である必要があります")
    private int rating;

    // レビュータイト（空文字許容・任意）
    private String title;

    // レビュー本文コメント
    @NotBlank(message = "comment は必須です")
    private String comment;

    // 画像URL（空文字許容・任意）
    private String image;
}
