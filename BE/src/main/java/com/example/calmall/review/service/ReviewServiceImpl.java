package com.example.calmall.review.service;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.orders.entity.Orders;
import com.example.calmall.orders.repository.OrdersRepository;
import com.example.calmall.product.entity.Product;
import com.example.calmall.product.repository.ProductRepository;
import com.example.calmall.review.dto.*;
import com.example.calmall.review.entity.Review;
import com.example.calmall.review.entity.ReviewImage;
import com.example.calmall.review.repository.ReviewRepository;
import com.example.calmall.review.repository.ReviewImageRepository;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * レビュー機能に関するサービス実装クラス
 * - レビュー投稿、取得（商品別・ユーザー別）、編集、削除、詳細取得を担当
 */
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;               // レビュー用リポジトリ
    private final ReviewImageRepository reviewImageRepository;     // レビュー画像用リポジトリ
    private final ReviewLikeRepository reviewLikeRepository;       // レビューいいね用リポジトリ
    private final UserRepository userRepository;                   // ユーザー用リポジトリ
    private final ProductRepository productRepository;             // 商品用リポジトリ
    private final OrdersRepository ordersRepository;               // 注文履歴用リポジトリ


    /**
     * レビュー投稿処理
     * - 購入1ヶ月以内のみ投稿可能
     * - 同一商品への複数レビューは禁止
     * - 削除済みレビューがある場合は再投稿不可
     * - 画像の review_id 紐付けは UPDATE のみで行う
     */
    @Override
    @Transactional
    public ResponseEntity<ApiResponseDto> postReview(ReviewRequestDto requestDto, String userId) {
        // 1. ユーザー取得
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが存在しません"));

        // 2. 商品取得
        Product product = productRepository.findByItemCode(requestDto.getItemCode())
                .orElseThrow(() -> new IllegalArgumentException("商品が存在しません"));

        // 3. 再投稿制限（論理削除済レビューが存在する場合）
        if (!reviewRepository.findByUser_UserIdAndProduct_ItemCodeAndDeletedTrue(userId, product.getItemCode()).isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("削除済レビューが存在するため再投稿できません"));
        }

        // 4. 既存レビュー投稿済みチェック（削除されていないレビュー）
        if (!reviewRepository.findByProduct_ItemCodeAndUser_UserIdAndDeletedFalse(product.getItemCode(), userId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("この商品には既にレビューを投稿済みです"));
        }

        // 5. 購入履歴チェック
        List<Orders> orders = ordersRepository.findByUser_UserIdAndProduct_ItemCode(userId, product.getItemCode());
        if (orders.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("未購入の商品にはレビューできません"));
        }

        // 6. 購入が1ヶ月以内かどうか判定
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        boolean purchasedWithinOneMonth = orders.stream()
                .anyMatch(order -> order.getCreatedAt().isAfter(oneMonthAgo));

        if (!purchasedWithinOneMonth) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("購入後1ヶ月以内のユーザーのみレビュー可能です"));
        }

        // 7. レビュー保存（新規作成）
        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(requestDto.getRating())
                .title(requestDto.getTitle())
                .comment(requestDto.getComment())
                .imageList(requestDto.getImageList())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleted(false)
                .build();

        Review savedReview = reviewRepository.save(review);

        // 8. 既存の画像レコードをレビューに紐付け（UPDATE のみ）
        if (requestDto.getImageList() != null && !requestDto.getImageList().isEmpty()) {
            for (String imageUrl : requestDto.getImageList()) {
                // 既存の未関連付け画像を検索（最新の1件のみ取得）
                Optional<ReviewImage> existingImageOpt = reviewImageRepository
                        .findTopByImageUrlAndReviewIsNullOrderByCreatedAtDesc(imageUrl);

                if (existingImageOpt.isEmpty()) {
                    System.out.println("[WARNING] 指定された画像が見つかりません: " + imageUrl);
                    continue;
                }

                ReviewImage imageToUpdate = existingImageOpt.get();
                imageToUpdate.setReview(savedReview);

                // contentTypeが未設定の場合に補完
                if (imageToUpdate.getContentType() == null) {
                    if (imageUrl.endsWith(".jpg") || imageUrl.endsWith(".jpeg")) {
                        imageToUpdate.setContentType("image/jpeg");
                    } else if (imageUrl.endsWith(".png")) {
                        imageToUpdate.setContentType("image/png");
                    } else {
                        imageToUpdate.setContentType("application/octet-stream");
                    }
                }

                System.out.println("[REVIEW] 画像関連付け完了: " + imageUrl + " -> reviewId: " + savedReview.getReviewId());
            }
        }

        return ResponseEntity.ok(new ApiResponseDto("success"));
    }

    /**
     * 商品別レビュー取得
     * - ページネーション対応
     * - 評価統計、マイレビュー、いいね状態を含む
     */
    @Override
    public ResponseEntity<ReviewListByItemResponseDto> getReviewsByItem(String itemCode, String userId, int page, int size) {
        // ページング取得
        Page<Review> reviewPage = reviewRepository.findByProduct_ItemCodeAndDeletedFalse(itemCode, PageRequest.of(page, size));
        List<Review> allReviews = reviewRepository.findByProduct_ItemCodeAndDeletedFalse(itemCode);

        // 評価統計データ作成
        Map<Integer, Long> ratingStatsMap = allReviews.stream()
                .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));

        List<ReviewListByItemResponseDto.RatingStat> ratingStats = new ArrayList<>();
        for (int i = 5; i >= 1; i--) {
            ratingStats.add(ReviewListByItemResponseDto.RatingStat.builder()
                    .score(i)
                    .count(ratingStatsMap.getOrDefault(i, 0L))
                    .build());
        }

        // レビューリスト変換（createdAtはLocalDateTimeのまま渡す）
        List<ReviewListByItemResponseDto.ReviewInfo> reviewInfos = reviewPage.getContent().stream()
                .map(r -> ReviewListByItemResponseDto.ReviewInfo.builder()
                        .reviewId(r.getReviewId())
                        .userNickname(r.getUser().getNickname())
                        .rating(r.getRating())
                        .title(r.getTitle())
                        .comment(r.getComment())
                        .imageList(r.getImageList())
                        .createdAt(r.getCreatedAt()) // 修正ポイント
                        .isLike(userId != null && reviewLikeRepository.existsByUserUserIdAndReviewReviewId(userId, r.getReviewId()))
                        .likeCount(reviewLikeRepository.countByReviewReviewId(r.getReviewId()))
                        .build())
                .collect(Collectors.toList());

        // マイレビュー取得
        ReviewListByItemResponseDto.MyReview myReview = null;
        if (userId != null) {
            List<Review> myList = reviewRepository.findByProduct_ItemCodeAndUser_UserIdAndDeletedFalse(itemCode, userId);
            if (!myList.isEmpty()) {
                Review r = myList.get(0);
                myReview = ReviewListByItemResponseDto.MyReview.builder()
                        .reviewId(r.getReviewId())
                        .rating(r.getRating())
                        .title(r.getTitle())
                        .comment(r.getComment())
                        .imageList(r.getImageList())
                        .createdAt(r.getCreatedAt()) // ★ 追加
                        .likeCount(reviewLikeRepository.countByReviewReviewId(r.getReviewId())) // ★ 追加
                        .build();
            }
        }

        return ResponseEntity.ok(ReviewListByItemResponseDto.builder()
                .message("success")
                .reviews(reviewInfos)
                .ratingStats(ratingStats)
                .myReview(myReview)
                .totalPages(reviewPage.getTotalPages())
                .currentPage(reviewPage.getNumber())
                .hasNext(reviewPage.hasNext())
                .totalElements(reviewPage.getTotalElements())
                .build());
    }

    /**
     * ユーザー別レビュー取得
     * - ページネーション対応
     */
    @Override
    public ResponseEntity<ReviewListByUserResponseDto> getReviewsByUser(String userId, int page, int size) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ReviewListByUserResponseDto.builder().message("ユーザーIDが必要です").build());
        }

        Page<Review> reviewPage = reviewRepository.findByUser_UserIdAndDeletedFalse(userId, PageRequest.of(page, size));

        List<ReviewListByUserResponseDto.UserReview> userReviews = reviewPage.getContent().stream().map(r ->
                ReviewListByUserResponseDto.UserReview.builder()
                        .reviewId(r.getReviewId())
                        .itemCode(r.getProduct().getItemCode())
                        .itemName(r.getProduct().getItemName())
                        .itemImage(r.getProduct().getImages().isEmpty() ? null : r.getProduct().getImages().get(0))
                        .rating(r.getRating())
                        .title(r.getTitle())
                        .comment(r.getComment())
                        .imageList(r.getImageList())
                        .createdAt(r.getCreatedAt()) // 修正
                        .likeCount(reviewLikeRepository.countByReviewReviewId(r.getReviewId())) // ★ 追加
                        .build()
        ).collect(Collectors.toList());

        return ResponseEntity.ok(ReviewListByUserResponseDto.builder()
                .message("success")
                .reviews(userReviews)
                .totalPages(reviewPage.getTotalPages())
                .currentPage(reviewPage.getNumber())
                .hasNext(reviewPage.hasNext())
                .totalElements(reviewPage.getTotalElements())
                .build());
    }

    /**
     * レビュー編集処理
     * - 本人のみ編集可能
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

        review.setRating(requestDto.getRating());
        review.setTitle(requestDto.getTitle());
        review.setComment(requestDto.getComment());
        review.setImageList(requestDto.getImageList());
        review.setUpdatedAt(LocalDateTime.now());

        // 画像の関連付け更新
        if (requestDto.getImageList() != null) {
            for (String fileName : requestDto.getImageList()) {
                List<ReviewImage> images = reviewImageRepository.findAllByImageUrl(fileName);
                for (ReviewImage image : images) {
                    image.setReview(review);
                }
            }
        }

        return ResponseEntity.ok(new ApiResponseDto("success"));
    }

    /**
     * レビュー削除（論理削除）
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

        review.setDeleted(true);
        review.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(new ApiResponseDto("success"));
    }

    /**
     * レビュー詳細取得
     */
    @Override
    public ResponseEntity<ReviewDetailResponseDto> getReviewDetail(Long reviewId, String userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("レビューが存在しません"));

        ReviewDetailResponseDto detail = ReviewDetailResponseDto.builder()
                .title(review.getTitle())
                .comment(review.getComment())
                .rating(review.getRating())
                .imageList(review.getImageList())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();

        return ResponseEntity.ok(detail);
    }
}