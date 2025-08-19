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


// レビュー機能に関するAPIコントローラー
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    // セッションからログインユーザーを取得
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
        // ServiceにはUserIDではなくUserを渡す実装へ移行も可能だが、投稿は既存のままでも可
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


    // 自分のレビュー一覧取得（フロントは userId を送らない）
    @GetMapping("/me")
    public ResponseEntity<ReviewListByUserResponseDto> getMyReviews(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size,
                                                                    HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (loginUser == null) {
            // 未ログイン時は 401
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // ServiceにUser渡す
        return reviewService.getReviewsByUser(loginUser, page, size);
    }

    // レビュー編集
    @PatchMapping("/{id}")
    public ResponseEntity<ReviewDetailResponseDto> updateReview(@PathVariable Long id,
                                                                @Valid @RequestBody ReviewUpdateRequestDto requestDto,
                                                                HttpServletRequest request) {
        User user = getLoginUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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

    // レビュー詳細取得（POST /api/reviews/{id} の仕様はそのまま踏襲）
    @PostMapping("/{id}")
    public ResponseEntity<ReviewDetailResponseDto> getReviewDetail(@PathVariable Long id,
                                                                   HttpServletRequest request) {
        User user = getLoginUser(request);
        String userId = (user != null) ? user.getUserId() : null;
        return reviewService.getReviewDetail(id, userId);
    }
}
