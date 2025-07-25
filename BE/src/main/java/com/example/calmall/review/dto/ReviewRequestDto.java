package com.example.calmall.review.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;

import java.util.List;

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

    // レビュータイトル（空文字許容・任意）
    private String title;

    // レビュー本文コメント
    @NotBlank(message = "comment は必須です")
    private String comment;

    // 画像URLのリスト（空、または複数画像URLを格納）
    private List<String> imageList;
}
