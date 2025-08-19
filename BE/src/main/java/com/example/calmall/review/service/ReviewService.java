package com.example.calmall.review.service;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.review.dto.*;
import com.example.calmall.user.entity.User;
import org.springframework.http.ResponseEntity;

/**
 * レビュー機能に関するサービスインターフェース
 * - 投稿、取得（商品・ユーザー別）、編集、削除を提供
 */
public interface ReviewService {

    // レビューを投稿する
    ResponseEntity<ApiResponseDto> postReview(ReviewRequestDto requestDto, String userId);


    // 商品に紐づくレビュー一覧を取得する
    ResponseEntity<ReviewListByItemResponseDto> getReviewsByItem(String itemCode, String userId, int page, int size);


    // ユーザーが投稿したレビュー一覧を取得する
    ResponseEntity<ReviewListByUserResponseDto> getReviewsByUser(User user, int page, int size);


    //  レビューを編集する
    ResponseEntity<ReviewDetailResponseDto> updateReview(Long reviewId, ReviewUpdateRequestDto requestDto, String userId);


    // レビューを削除する（論理削除）
    ResponseEntity<ApiResponseDto> deleteReview(Long reviewId, String userId);


    // レビュー詳細を取得する
    ResponseEntity<ReviewDetailResponseDto> getReviewDetail(Long reviewId, String currentUserId);
}
