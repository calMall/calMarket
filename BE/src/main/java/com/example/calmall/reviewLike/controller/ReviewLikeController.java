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

/**
 * レビューの「いいね」機能に関するAPIを提供するコントローラークラス
 * - いいね追加／削除（トグル）
 * - 特定レビューに対するいいね一覧取得
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReviewLikeController {

    // ビジネスロジックを扱うサービス層を注入
    private final ReviewLikeService reviewLikeService;

    /**
     * 「いいね」トグルAPI（POST /api/review-likes）
     * - ログインユーザーが指定レビューに「いいね」または「いいね解除」する
     * - トグル方式で、既に押していれば削除、まだなら追加
     *
     * @param requestDto reviewId のみを含むリクエストDTO
     * @param session 現在のセッション（ログイン中ユーザーを取得するため）
     * @return 成功時：200, 未ログイン時：401, 処理失敗時：400
     */
    @PostMapping("/review-likes")
    public ResponseEntity<ApiResponseDto> toggleLike(
            @RequestBody ReviewLikeRequestDto requestDto,
            HttpSession session) {

        // セッションからログインユーザーを取得
        User loginUser = (User) session.getAttribute("user");

        // 未ログインの場合は 401 を返却
        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDto("fail"));
        }

        // ログインユーザーと指定レビューIDでトグル処理を実行
        boolean result = reviewLikeService.toggleLike(loginUser, requestDto.getReviewId());

        // 成功／失敗でレスポンスを分岐
        if (result) {
            return ResponseEntity.ok(new ApiResponseDto("success"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("fail"));
        }
    }

    /**
     * 指定レビューの「いいね」一覧取得API（GET /api/review-likes?reviewId=xxx）
     * - 特定レビューに「いいね」したユーザーの userId, nickname を一覧で返却する
     *
     * @param reviewId クエリパラメータで受け取るレビューID
     * @return 成功時："success"＋いいねしたユーザー一覧 / 失敗時："fail"
     */
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