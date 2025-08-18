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
 * - 投稿者の userId と ログインユーザー本人かどうかを示す isOwner を追加
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDetailResponseDto {

    // 投稿者のユーザーID（UUID形式）
    private String userId;

    // 投稿者のニックネーム
    private String userNickname;

    // 商品コード
    private String itemCode;

    // 商品名
    private String itemName;

    // 商品画像URLのリスト
    private List<String> imageUrls;

    private String title; // レビュータイトル
    private String comment; // コメント
    private int rating; // 評価（1〜5）
}
