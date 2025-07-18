package com.example.calmall.reviewLike.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// レビューに対して「いいね」を追加・トグルするリクエストDTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewLikeRequestDto {

    // ユーザーID
    private Long userId;

    // レビューID
    private Long reviewId;
}
