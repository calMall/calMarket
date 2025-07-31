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
 * レビュー機能に関するAPIコントローラー
 * - 認証・権限管理はControllerのみ
 * - サービスにはuserId等だけ渡す
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    // セッションからログインユーザーを取得（共通メソッド）
    private User getLoginUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return (session != null) ? (User) session.getAttribute("user") : null;
    }

    // レビュー投稿
    @PostMapping
    public ResponseEntity<ApiResponseDto> postReview(@Valid @RequestBody ReviewRequestDto requestDto,
                                                     HttpServletRequest request) {
        User user = getLoginUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDto("ログインが必要です"));
        }
        return reviewService.postReview(requestDto, user.getUserId());
    }

    // 商品別レビュー取得
    @GetMapping(params = "itemCode")
    public ResponseEntity<ReviewListByItemResponseDto> getReviewsByItem(@RequestParam String itemCode,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "10") int size,
                                                                        HttpServletRequest request) {
        User user = getLoginUser(request);
        String userId = (user != null) ? user.getUserId() : null;
        return reviewService.getReviewsByItem(itemCode, userId, page, size);
    }

    // ユーザー別レビュー取得
    @GetMapping(params = "userId")
    public ResponseEntity<ReviewListByUserResponseDto> getReviewsByUser(@RequestParam String userId,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "10") int size,
                                                                        HttpServletRequest request) {
        // ※自分のレビュー一覧取得のみ認証（他人は不可なら要判定、要件に応じて）
        User loginUser = getLoginUser(request);
        if (loginUser == null || !loginUser.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null); // 仕様に応じてメッセージ変更
        }
        return reviewService.getReviewsByUser(userId, page, size);
    }

    // レビュー編集
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

    // レビュー削除
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

    // レビュー詳細取得
    @PostMapping("/{id}")
    public ResponseEntity<ReviewDetailResponseDto> getReviewDetail(@PathVariable Long id,
                                                                   HttpServletRequest request) {
        User user = getLoginUser(request);
        String userId = (user != null) ? user.getUserId() : null;
        return reviewService.getReviewDetail(id, userId);
    }
}
