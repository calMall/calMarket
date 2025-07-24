package com.example.calmall.review.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;

/**
 * レビュー投稿用リクエストDTOクラス
 * ユーザーが商品レビューを投稿する際に必要なデータを受け取る
 */
@Getter
public class ReviewRequestDto {

    // 商品のコード（楽天APIの商品識別子）
    @NotBlank(message = "itemCode は必須です")
    private String itemCode;

    // レビューを投稿するユーザーのID（UUIDベースのString）
    @NotBlank(message = "userId は必須です")
    private String userId;

    // レビュー評価（1〜5）
    @Min(value = 1, message = "rating は1以上である必要があります")
    @Max(value = 5, message = "rating は5以下である必要があります")
    private int rating;

    // レビュータイトル
    @NotBlank(message = "title は必須です")
    private String title;

    // レビュー本文コメント
    @NotBlank(message = "comment は必須です")
    private String comment;

    // 画像URL（空文字許容・任意）
    private String image;
}