package com.example.calmall.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

/**
 * ユーザー詳細取得時のレスポンスDTO
 */
@Data
@AllArgsConstructor
public class UserDetailResponseDto {

    private String message; // "success" または "fail"
    private int point; // ユーザーの所持ポイント

    // 最新の注文履歴（最大10件）
    private List<OrderSummary> orders;

    // 最新のレビュー履歴（最大10件）
    private List<ReviewSummary> reviews;

    /**
     * 注文概要用の内部クラス
     */
    @Data
    @AllArgsConstructor
    public static class OrderSummary {
        private Long id;         // 注文ID
        private String imageUrl; // 商品画像URL
    }

    /**
     * レビュー概要用の内部クラス
     */
    @Data
    @AllArgsConstructor
    public static class ReviewSummary {
        private Long id;         // レビューID
        private String title;    // レビュータイトル
        private String createdAt; // 作成日時（ISO形式など）
        private int score;       // レビュー点数
        private String content;  // レビュー内容
    }
}