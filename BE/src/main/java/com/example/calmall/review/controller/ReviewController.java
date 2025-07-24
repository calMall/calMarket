package com.example.calmall.review.controller;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.review.dto.*;
import com.example.calmall.review.service.ReviewService;
import com.example.calmall.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * レビュー機能に関するコントローラークラス
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * レビュー投稿API（1ヶ月以内購入者のみ）
     */
    @PostMapping
    public ResponseEntity<ApiResponseDto> postReview(@Valid @RequestBody ReviewRequestDto requestDto,
                                                     @SessionAttribute("user") User loginUser) {
        return reviewService.postReview(requestDto, loginUser.getUserId());
    }

    /**
     * 商品ごとのレビュー取得API（ページネーション・マイレビュー・評価統計付き）
     */
    @GetMapping(params = "itemCode")
    public ResponseEntity<ReviewListByItemResponseDto> getReviewsByItem(@RequestParam String itemCode,
                                                                        @RequestParam(required = false) String userId,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "10") int size) {
        return reviewService.getReviewsByItem(itemCode, userId, page, size);
    }

    /**
     * ユーザーごとのレビュー取得API（ページネーション付き）
     */
    @GetMapping(params = "userId")
    public ResponseEntity<ReviewListByUserResponseDto> getReviewsByUser(@RequestParam String userId,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "10") int size) {
        return reviewService.getReviewsByUser(userId, page, size);
    }

    /**
     * レビュー編集API（本人のみ編集可）
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponseDto> updateReview(@PathVariable Long id,
                                                       @Valid @RequestBody ReviewUpdateRequestDto requestDto,
                                                       @SessionAttribute("user") User loginUser) {
        return reviewService.updateReview(id, requestDto, loginUser.getUserId());
    }

    /**
     * レビュー削除API（本人のみ削除可）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto> deleteReview(@PathVariable Long id,
                                                       @SessionAttribute("user") User loginUser) {
        return reviewService.deleteReview(id, loginUser.getUserId());
    }
}
