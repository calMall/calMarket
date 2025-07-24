package com.example.calmall.review.service;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.review.dto.*;
import org.springframework.http.ResponseEntity;

public interface ReviewService {

    // レビュー投稿（購入後1ヶ月以内ユーザーのみ）
    ResponseEntity<ApiResponseDto> postReview(ReviewRequestDto requestDto);

    // 商品ごとのレビュー一覧取得（マイレビュー、統計含む）
    ResponseEntity<ReviewListByItemResponseDto> getReviewsByItem(String itemCode, String userId, int page, int size);

    // ユーザーごとのレビュー一覧取得
    ResponseEntity<ReviewListByUserResponseDto> getReviewsByUser(String userId, int page, int size);

    // レビュー編集
    ResponseEntity<ApiResponseDto> updateReview(Long reviewId, ReviewUpdateRequestDto requestDto);

    // レビュー削除
    ResponseEntity<ApiResponseDto> deleteReview(Long reviewId);
}