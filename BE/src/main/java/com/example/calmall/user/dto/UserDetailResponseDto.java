package com.example.calmall.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// ユーザー詳細取得APIのレスポンスDTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailResponseDto {

    // 処理結果メッセージ（"success" または "fail"）
    private String message;

    // ユーザーの現在の所持ポイント（例：120）
    private int point;

    // 住所
    private List<String> deliveryAddresses;

    // 注文履歴（最新10件まで）
    // 商品の画像と注文IDのみを簡易表示
    private List<OrderSummary> orders;

    // レビュー履歴（最新10件まで）
    // 投稿したレビューの概要を表示
    private List<ReviewSummary> reviews;

    // 注文履歴の1件分を表すDTO（商品画像つき）
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSummary {

        // 注文ID
        private Long id;

        // 商品の代表画像URL（サムネイル用途）
        private String imageUrl;
    }

    // レビュー履歴の1件分を表すDTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewSummary {

        // レビューID
        private Long id;

        // レビューのタイトル（例：最高の商品」）
        private String title;

        // レビュー投稿日時（ISO形式、例："2025-07-17T10:30:00"）
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private String createdAt;

        // 評価スコア（1〜5）
        private int score;

        // 本文コメント
        private String content;

        // 配送先住所リスト（例：["東京都新宿区3-1-1", "大阪府大阪市北区1-2-3"]）
        private List<String> deliveryAddresses;
    }
}
