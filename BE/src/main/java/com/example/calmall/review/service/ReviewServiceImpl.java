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
 * ReviewService の実装クラス
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
     * レビュー投稿（1ヶ月以内の購入ユーザー限定、同一商品に対し1回のみ投稿可能）
     */
    @Override
    @Transactional
    public ResponseEntity<ApiResponseDto> postReview(ReviewRequestDto requestDto, String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが存在しません"));
        Product product = productRepository.findByItemCode(requestDto.getItemCode())
                .orElseThrow(() -> new IllegalArgumentException("商品が存在しません"));

        // 削除済レビューが存在するかチェック（再投稿禁止）
        Optional<Review> deleted = reviewRepository.findByUser_UserIdAndProduct_ItemCodeAndDeletedIsTrue(userId, product.getItemCode());
        if (deleted.isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("削除済レビューが存在するため再投稿できません"));
        }

        // 既に投稿済レビューがあるかチェック（1商品に1回制限）
        Optional<Review> existing = reviewRepository.findByProduct_ItemCodeAndUser_UserId(product.getItemCode(), userId);
        if (existing.isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("この商品には既にレビューを投稿済みです"));
        }

        // 購入1ヶ月以内チェック
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        boolean hasRecentOrder = ordersRepository.existsByUser_UserIdAndProduct_ItemCodeAndCreatedAtAfter(
                user.getUserId(), product.getItemCode(), oneMonthAgo);

        if (!hasRecentOrder) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("購入後1ヶ月以内のユーザーのみレビュー可能です"));
        }

        // レビュー新規作成
        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(requestDto.getRating())
                .title(requestDto.getTitle())
                .comment(requestDto.getComment())
                .image(requestDto.getImage())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleted(false) // 論理削除フラグ（新規なので false）
                .build();

        reviewRepository.save(review);
        return ResponseEntity.ok(new ApiResponseDto("success"));
    }

    /**
     * 商品別レビュー一覧取得（統計情報・マイレビュー付き）
     */
    @Override
    public ResponseEntity<ReviewListByItemResponseDto> getReviewsByItem(String itemCode, String userId, int page, int size) {
        Page<Review> reviewPage = reviewRepository.findByProduct_ItemCode(itemCode, PageRequest.of(page, size));
        List<Review> allReviews = reviewRepository.findByProduct_ItemCode(itemCode);

        double average = allReviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        long count = allReviews.size();

        ReviewListByItemResponseDto.RatingStat stats = ReviewListByItemResponseDto.RatingStat.builder()
                .average(average)
                .count(count)
                .build();

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
     * ユーザー別レビュー一覧取得
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
     * レビュー編集（本人のみ可能）
     */
    @Override
    @Transactional
    public ResponseEntity<ApiResponseDto> updateReview(Long reviewId, ReviewUpdateRequestDto requestDto, String userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("レビューが存在しません"));

        if (!review.getUser().getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseDto("本人のみ編集可能です"));
        }

        // 追加：評価も更新できるようにする
        review.setRating(requestDto.getRating());

        review.setTitle(requestDto.getTitle());
        review.setComment(requestDto.getComment());
        review.setImage(requestDto.getImage());
        review.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(new ApiResponseDto("success"));
    }

    /**
     * レビュー削除（論理削除・本人のみ）
     */
    @Override
    @Transactional
    public ResponseEntity<ApiResponseDto> deleteReview(Long reviewId, String userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("レビューが存在しません"));

        if (!review.getUser().getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseDto("本人のみ削除可能です"));
        }

        // 論理削除（DB上に残して再投稿防止）
        review.setDeleted(true);
        review.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(new ApiResponseDto("success"));
    }
}
