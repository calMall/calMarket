package com.example.calmall.review.service;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.review.dto.*;
import org.springframework.http.ResponseEntity;

public interface ReviewService {

    /**
     * レビュー投稿（セッションで取得したuserIdを使用）
     */
    ResponseEntity<ApiResponseDto> postReview(ReviewRequestDto requestDto, String userId);

    /**
     * 商品ごとのレビュー一覧取得（統計・マイレビュー付き）
     */
    ResponseEntity<ReviewListByItemResponseDto> getReviewsByItem(String itemCode, String userId, int page, int size);

    /**
     * ユーザーごとのレビュー一覧取得（ページネーション付き）
     */
    ResponseEntity<ReviewListByUserResponseDto> getReviewsByUser(String userId, int page, int size);

    /**
     * レビュー編集（本人のみ編集可）
     */
    ResponseEntity<ApiResponseDto> updateReview(Long reviewId, ReviewUpdateRequestDto requestDto, String userId);

    /**
     * レビュー削除（本人のみ削除可）
     */
    ResponseEntity<ApiResponseDto> deleteReview(Long reviewId, String userId);
}
