package com.example.calmall.review.service;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.orders.repository.OrdersRepository;
import com.example.calmall.product.entity.Product;
import com.example.calmall.product.repository.ProductRepository;
import com.example.calmall.review.dto.*;
import com.example.calmall.review.entity.Review;
import com.example.calmall.review.repository.ReviewRepository;
import com.example.calmall.reviewLike.repository.ReviewLikeRepository;
import com.example.calmall.user.entity.User;
import com.example.calmall.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ReviewServiceの実装クラス
 */
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrdersRepository ordersRepository;

    /**
     * レビュー投稿（1ヶ月以内の購入ユーザーのみ）
     */
    @Override
    @Transactional
    public ResponseEntity<ApiResponseDto> postReview(ReviewRequestDto requestDto) {
        // ユーザーと商品取得
        User user = userRepository.findByUserId(requestDto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが存在しません"));
        Product product = productRepository.findByItemCode(requestDto.getItemCode())
                .orElseThrow(() -> new IllegalArgumentException("商品が存在しません"));

        // 購入履歴の1ヶ月以内チェック
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        boolean hasRecentOrder = ordersRepository.existsByUser_UserIdAndProduct_ItemCodeAndCreatedAtAfter(
                user.getUserId(), product.getItemCode(), oneMonthAgo);

        if (!hasRecentOrder) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("購入後1ヶ月以内のユーザーのみレビュー可能です"));
        }

        // レビュー作成
        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(requestDto.getRating())
                .title(requestDto.getTitle())
                .comment(requestDto.getComment())
                .image(requestDto.getImage())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        reviewRepository.save(review);
        return ResponseEntity.ok(new ApiResponseDto("success"));
    }

    /**
     * 商品ごとのレビュー一覧取得（統計・マイレビュー付き）
     */
    @Override
    public ResponseEntity<ReviewListByItemResponseDto> getReviewsByItem(String itemCode, String userId, int page, int size) {
        Page<Review> reviewPage = reviewRepository.findByProduct_ItemCode(itemCode, PageRequest.of(page, size));
        List<Review> allReviews = reviewRepository.findByProduct_ItemCode(itemCode);

        // 統計情報
        double average = allReviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        long count = allReviews.size();
        ReviewListByItemResponseDto.RatingStat stats = ReviewListByItemResponseDto.RatingStat.builder()
                .average(average)
                .count(count)
                .build();

        // レビューリスト構築
        List<ReviewListByItemResponseDto.ReviewInfo> reviews = reviewPage.getContent().stream().map(r -> {
            boolean liked = userId != null && reviewLikeRepository.existsByUserUserIdAndReviewReviewId(userId, r.getReviewId());
            long likeCount = reviewLikeRepository.countByReviewReviewId(r.getReviewId());
            return ReviewListByItemResponseDto.ReviewInfo.builder()
                    .reviewId(r.getReviewId())
                    .userNickname(r.getUser().getNickname())
                    .rating(r.getRating())
                    .title(r.getTitle())
                    .comment(r.getComment())
                    .image(r.getImage())
                    .createdAt(r.getCreatedAt().toString())
                    .liked(liked)
                    .likeCount(likeCount)
                    .build();
        }).collect(Collectors.toList());

        // 自分のレビュー
        ReviewListByItemResponseDto.MyReview myReview = null;
        if (userId != null) {
            Optional<Review> my = reviewRepository.findByProduct_ItemCodeAndUser_UserId(itemCode, userId);
            if (my.isPresent()) {
                Review r = my.get();
                myReview = ReviewListByItemResponseDto.MyReview.builder()
                        .rating(r.getRating())
                        .title(r.getTitle())
                        .comment(r.getComment())
                        .image(r.getImage())
                        .createdAt(r.getCreatedAt().toString())
                        .build();
            }
        }

        return ResponseEntity.ok(ReviewListByItemResponseDto.builder()
                .message("success")
                .reviews(reviews)
                .stats(stats)
                .myReview(myReview)
                .build());
    }

    /**
     * ユーザーごとのレビュー取得（ページネーション付き）
     */
    @Override
    public ResponseEntity<ReviewListByUserResponseDto> getReviewsByUser(String userId, int page, int size) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ReviewListByUserResponseDto.builder().message("ユーザーIDが必要です").build());
        }

        Page<Review> reviewPage = reviewRepository.findByUser_UserId(userId, PageRequest.of(page, size));

        List<ReviewListByUserResponseDto.UserReview> reviews = reviewPage.getContent().stream().map(r ->
                ReviewListByUserResponseDto.UserReview.builder()
                        .reviewId(r.getReviewId())
                        .itemCode(r.getProduct().getItemCode())
                        .itemName(r.getProduct().getItemName())
                        .itemImage(r.getProduct().getImages().isEmpty() ? null : r.getProduct().getImages().get(0))
                        .rating(r.getRating())
                        .title(r.getTitle())
                        .comment(r.getComment())
                        .image(r.getImage())
                        .createdAt(r.getCreatedAt().toString())
                        .build()
        ).collect(Collectors.toList());

        return ResponseEntity.ok(ReviewListByUserResponseDto.builder()
                .message("success")
                .reviews(reviews)
                .build());
    }

    /**
     * レビュー編集（本人のみ）
     */
    @Override
    @Transactional
    public ResponseEntity<ApiResponseDto> updateReview(Long reviewId, ReviewUpdateRequestDto requestDto) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("レビューが存在しません"));

        // 本人チェックは別途ログイン処理が必要

        review.setTitle(requestDto.getTitle());
        review.setComment(requestDto.getComment());
        review.setImage(requestDto.getImage());
        review.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(new ApiResponseDto("success"));
    }

    /**
     * レビュー削除（本人のみ）
     */
    @Override
    @Transactional
    public ResponseEntity<ApiResponseDto> deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("レビューが存在しません"));

        reviewRepository.delete(review);
        return ResponseEntity.ok(new ApiResponseDto("success"));
    }
}