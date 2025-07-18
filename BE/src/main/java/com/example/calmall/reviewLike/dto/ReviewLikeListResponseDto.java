package com.example.calmall.reviewLike.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// 特定レビューに対する「いいね」一覧のレスポンスDTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewLikeListResponseDto {

    // レスポンスメッセージ（"success" または "fail"）
    private String message;

    // いいねしたユーザーのリスト
    private List<LikeUser> likes;

    // いいねしたユーザー情報
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LikeUser {

        // ユーザーID
        private Long userId;

        // ユーザーのニックネーム
        private String nickname;
    }
}
