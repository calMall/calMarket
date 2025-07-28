package com.example.calmall.review.controller;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.review.dto.*;
import com.example.calmall.review.service.ReviewService;
import com.example.calmall.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * レビュー機能に関するAPIを提供するコントローラークラス
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * セッションからログインユーザーを取得する共通メソッド
     */
    private User getLoginUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return (session != null) ? (User) session.getAttribute("user") : null;
    }

    /**
     * レビュー投稿API
     * - ログイン必須
     * - 購入後1ヶ月以内のユーザーのみ投稿可能
     */
    @PostMapping
    public ResponseEntity<ApiResponseDto> postReview(@Valid @RequestBody ReviewRequestDto requestDto,
                                                     HttpServletRequest request) {
        User user = getLoginUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDto("ログインが必要です"));
        }
        System.out.println(requestDto);
        return reviewService.postReview(requestDto, user.getUserId());
    }

    /**
     * 商品ごとのレビュー一覧取得API
     * - ページネーション対応
     * - ログイン中ユーザーのマイレビュー、点数別統計情報を含む
     */
    @GetMapping(params = "itemCode")
    public ResponseEntity<ReviewListByItemResponseDto> getReviewsByItem(@RequestParam String itemCode,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "10") int size,
                                                                        HttpServletRequest request) {
        User user = getLoginUser(request);
        String userId = (user != null) ? user.getUserId() : null;

        return reviewService.getReviewsByItem(itemCode, userId, page, size);
    }

    /**
     * ユーザーごとのレビュー一覧取得API
     * - クエリパラメータで userId を指定
     * - ページネーション対応
     */
    @GetMapping(params = "userId")
    public ResponseEntity<ReviewListByUserResponseDto> getReviewsByUser(@RequestParam String userId,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "10") int size) {
        return reviewService.getReviewsByUser(userId, page, size);
    }

    /**
     * レビュー編集API
     * - ログイン必須
     * - 本人投稿のみ編集可能
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponseDto> updateReview(@PathVariable Long id,
                                                       @Valid @RequestBody ReviewUpdateRequestDto requestDto,
                                                       HttpServletRequest request) {
        User user = getLoginUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDto("ログインが必要です"));
        }

        return reviewService.updateReview(id, requestDto, user.getUserId());
    }

    /**
     * レビュー削除API
     * - ログイン必須
     * - 本人投稿のみ削除可能（論理削除）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto> deleteReview(@PathVariable Long id,
                                                       HttpServletRequest request) {
        User user = getLoginUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDto("ログインが必要です"));
        }

        return reviewService.deleteReview(id, user.getUserId());
    }

    /**
     * レビュー詳細取得API
     * - POST /api/reviews/{id}
     * - レビューIDに基づいて詳細情報を返す
     */
    @PostMapping("/{id}")
    public ResponseEntity<ReviewDetailResponseDto> getReviewDetail(@PathVariable Long id,
                                                                   HttpServletRequest request) {
        User user = getLoginUser(request);
        String userId = (user != null) ? user.getUserId() : null;

        return reviewService.getReviewDetail(id, userId);
    }
}
