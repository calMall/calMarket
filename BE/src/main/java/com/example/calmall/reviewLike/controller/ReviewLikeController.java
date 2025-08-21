package com.example.calmall.reviewLike.controller;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.reviewLike.dto.ReviewLikeListResponseDto;
import com.example.calmall.reviewLike.dto.ReviewLikeRequestDto;
import com.example.calmall.reviewLike.service.ReviewLikeService;
import com.example.calmall.user.entity.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//レビューのいいね機能に関するAPIを提供するコントローラークラス
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReviewLikeController {

    private final ReviewLikeService reviewLikeService;

     //　トグルAPI（トグル方式で、既に押していると削除、まだなら追加）
    @PostMapping("/review-likes")
    public ResponseEntity<ApiResponseDto> toggleLike(
            @RequestBody ReviewLikeRequestDto requestDto,
            HttpSession session) {

        // セッションからログインユーザーを取得
        User loginUser = (User) session.getAttribute("user");

        // 未ログインの場合は401
        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDto("fail"));
        }

        // ログインユーザーと指定レビューIDでトグル処理を実行
        boolean result = reviewLikeService.toggleLike(loginUser, requestDto.getReviewId());

        if (result) {
            return ResponseEntity.ok(new ApiResponseDto("success"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("fail"));
        }
    }


    //  レビューのいいね一覧取得API
    @GetMapping("/review-likes")
    public ResponseEntity<ReviewLikeListResponseDto> getLikes(@RequestParam("reviewId") Long reviewId) {
        try {
            // 指定レビューIDに「いいね」したユーザーリストを取得
            List<ReviewLikeListResponseDto.LikeUser> likes = reviewLikeService.getLikesByReviewId(reviewId);

            // 成功時レスポンス
            return ResponseEntity.ok(new ReviewLikeListResponseDto("success", likes));
        } catch (Exception e) {
            // エラー時レスポンス（空リスト＋fail）
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ReviewLikeListResponseDto("fail", List.of()));
        }
    }
}
