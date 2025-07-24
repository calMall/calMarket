package com.example.calmall.review.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;

/**
 * レビュー更新用リクエストDTOクラス
 * ユーザーが自分の投稿済みレビューを編集する際に使用する
 */
@Getter
public class ReviewUpdateRequestDto {

    // 編集後のレビュータイトル
    @NotBlank(message = "title は必須です")
    private String title;

    // 編集後のレビュー本文
    @NotBlank(message = "comment は必須です")
    private String comment;

    // 編集後の画像URL（空文字可）
    private String image;
}
