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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * レビュー機能に関するサービス実装クラス
 * - sessionやuser entityを直接参照しない（認証はController側管理）
 * - 全ての業務ロジックのみを担当
 */
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrdersRepository ordersRepository;

    /**
     * レビュー投稿（認証済userIdのみ受け取る）
     * - 画像は既存DBに登録済みのみ紐付け可
     * - 画像は「未紐付け状態（review=null）」のみ紐付けし、重複insertを禁止
     */
    @Override
    @Transactional
    public ResponseEntity<ApiResponseDto> postReview(ReviewRequestDto requestDto, String userId) {
        // ユーザー存在チェック
        Optional<User> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("ユーザーが存在しません"));
        }
        User user = userOpt.get();

        // 商品存在チェック
        Optional<Product> productOpt = productRepository.findByItemCode(requestDto.getItemCode());
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("商品が存在しません"));
        }
        Product product = productOpt.get();

        // 過去に削除済みレビューが存在する場合、再投稿不可
        if (!reviewRepository.findByUser_UserIdAndProduct_ItemCodeAndDeletedTrue(userId, product.getItemCode()).isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("削除済レビューが存在するため再投稿できません"));
        }
        // 既に未削除のレビューがある場合も不可
        if (!reviewRepository.findByProduct_ItemCodeAndUser_UserIdAndDeletedFalse(product.getItemCode(), userId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("この商品には既にレビューを投稿済みです"));
        }

        // 購入履歴チェック
        List<Orders> orders = ordersRepository.findByUser_UserIdAndProduct_ItemCode(userId, product.getItemCode());
        if (orders.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("未購入の商品にはレビューできません"));
        }

        // 購入1ヶ月以内チェック
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        boolean purchasedWithinOneMonth = orders.stream()
                .anyMatch(order -> order.getCreatedAt().isAfter(oneMonthAgo));
        if (!purchasedWithinOneMonth) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("購入後1ヶ月以内のユーザーのみレビュー可能です"));
        }

        // レビューエンティティ生成
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

        // DBに保存（ReviewId発番）
        Review savedReview = reviewRepository.save(review);

        // 画像リストが指定されている場合、未紐付け画像（review=null）だけを1件だけ紐付け、残りは削除
        if (requestDto.getImageList() != null && !requestDto.getImageList().isEmpty()) {
            Set<String> uniqueImageUrls = new LinkedHashSet<>(requestDto.getImageList());
            for (String imageUrl : uniqueImageUrls) {
                // すでにこのレビューに紐付け済みならスキップ
                if (reviewImageRepository.existsByImageUrlAndReview(imageUrl, savedReview)) {
                    System.out.println("[SKIP] このレビューには既に紐付け済み: " + imageUrl);
                    continue;
                }

                // 未紐付け（review=null）の同一imageUrl画像を全件取得
                List<ReviewImage> unlinkedImages = reviewImageRepository.findByImageUrlAndReviewIsNull(imageUrl);

                if (!unlinkedImages.isEmpty()) {
                    // 一番古い画像1件だけレビューに紐付け
                    ReviewImage imageToBind = unlinkedImages.get(0);
                    imageToBind.setReview(savedReview);
                    reviewImageRepository.save(imageToBind);

                    // 残りの未紐付け画像（ゴミデータ）は物理削除
                    for (int i = 1; i < unlinkedImages.size(); i++) {
                        reviewImageRepository.delete(unlinkedImages.get(i));
                        System.out.println("[DELETE] 重複未紐付け画像を自動削除: id=" + unlinkedImages.get(i).getId());
                    }
                    System.out.println("[LINKED] 画像をレビューに紐付け完了: " + imageUrl + " -> reviewId: " + savedReview.getReviewId());
                } else {
                    // 未紐付け画像が存在しない場合は何もしない
                    System.out.println("[SKIP] 未紐付け画像が存在しない: " + imageUrl + "（新規登録は禁止）");
                }
            }
        }
        return ResponseEntity.ok(new ApiResponseDto("success"));
    }

    /**
     * 商品別レビュー取得（ログイン有無はControllerで判定、userIdのみ受け取る）
     */
    @Override
    public ResponseEntity<ReviewListByItemResponseDto> getReviewsByItem(String itemCode, String userId, int page, int size) {
        Page<Review> reviewPage = reviewRepository.findByProduct_ItemCodeAndDeletedFalse(
                itemCode, PageRequest.of(page, size)
        );
        List<Review> allReviews = reviewRepository.findByProduct_ItemCodeAndDeletedFalse(itemCode);

        Map<Integer, Long> ratingStatsMap = allReviews.stream()
                .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));
        List<ReviewListByItemResponseDto.RatingStat> ratingStats = new ArrayList<>();
        for (int i = 5; i >= 1; i--) {
            ratingStats.add(ReviewListByItemResponseDto.RatingStat.builder()
                    .score(i)
                    .count(ratingStatsMap.getOrDefault(i, 0L))
                    .build());
        }

        List<ReviewListByItemResponseDto.ReviewInfo> reviewInfos = reviewPage.getContent().stream()
                .map(r -> ReviewListByItemResponseDto.ReviewInfo.builder()
                        .reviewId(r.getReviewId())
                        .userNickname(r.getUser().getNickname())
                        .userId(r.getUser().getUserId())
                        .rating(r.getRating())
                        .title(r.getTitle())
                        .comment(r.getComment())
                        .imageList(r.getImageList())
                        .createdAt(r.getCreatedAt())
                        .isLike(userId != null && reviewLikeRepository.existsByUserUserIdAndReviewReviewId(userId, r.getReviewId()))
                        .likeCount(reviewLikeRepository.countByReviewReviewId(r.getReviewId()))
                        .isOwner(userId != null && userId.equals(r.getUser().getUserId()))
                        .build())
                .collect(Collectors.toList());

        ReviewListByItemResponseDto.MyReview myReview = null;
        if (userId != null) {
            List<Review> myList = reviewRepository.findByProduct_ItemCodeAndUser_UserIdAndDeletedFalse(itemCode, userId);
            if (!myList.isEmpty()) {
                Review r = myList.get(0);
                myReview = ReviewListByItemResponseDto.MyReview.builder()
                        .reviewId(r.getReviewId())
                        .userId(r.getUser().getUserId())
                        .rating(r.getRating())
                        .title(r.getTitle())
                        .comment(r.getComment())
                        .imageList(r.getImageList())
                        .createdAt(r.getCreatedAt())
                        .isLike(reviewLikeRepository.existsByUserUserIdAndReviewReviewId(userId, r.getReviewId()))
                        .isOwner(userId.equals(r.getUser().getUserId()))
                        .likeCount(reviewLikeRepository.countByReviewReviewId(r.getReviewId()))
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
     * ユーザー別レビュー取得（Controllerで認証済userIdのみ受け取る）
     */
    @Override
    public ResponseEntity<ReviewListByUserResponseDto> getReviewsByUser(String userId, int page, int size) {
        Page<Review> reviewPage = reviewRepository.findByUser_UserIdAndDeletedFalse(userId, PageRequest.of(page, size));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<ReviewListByUserResponseDto.UserReview> userReviews = reviewPage.getContent().stream()
                .map(r -> ReviewListByUserResponseDto.UserReview.builder()
                        .reviewId(r.getReviewId())
                        .itemCode(r.getProduct().getItemCode())
                        .itemName(r.getProduct().getItemName())
                        .itemImage(r.getProduct().getImages().isEmpty() ? null : r.getProduct().getImages().get(0))
                        .rating(r.getRating())
                        .title(r.getTitle())
                        .comment(r.getComment())
                        .imageList(r.getImageList())
                        .createdAt(r.getCreatedAt())
                        .isLike(reviewLikeRepository.existsByUserUserIdAndReviewReviewId(userId, r.getReviewId()))
                        .likeCount(reviewLikeRepository.countByReviewReviewId(r.getReviewId()))
                        .isOwner(true)
                        .build())
                .collect(Collectors.toList());

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
     * レビュー編集（Controllerで認証済userIdのみ受け取る）
     * - 画像の再紐付けも同様、既存画像且つ未紐付けのみ可
     */
    @Override
    @Transactional
    public ResponseEntity<ApiResponseDto> updateReview(Long reviewId, ReviewUpdateRequestDto requestDto, String userId) {
        // レビュー存在チェック
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("レビューが存在しません"));

        // 論理削除済みは編集不可
        if (review.isDeleted()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("削除済みレビューは編集できません"));
        }
        // 本人のみ編集可能
        if (!review.getUser().getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseDto("本人のみ編集可能です"));
        }

        // レビュー内容更新
        review.setRating(requestDto.getRating());
        review.setTitle(requestDto.getTitle());
        review.setComment(requestDto.getComment());
        review.setImageList(requestDto.getImageList());
        review.setUpdatedAt(LocalDateTime.now());

        // 画像の再紐付け処理
        if (requestDto.getImageList() != null && !requestDto.getImageList().isEmpty()) {
            Set<String> uniqueImageUrls = new LinkedHashSet<>(requestDto.getImageList());
            for (String imageUrl : uniqueImageUrls) {
                // 既にこのレビューに紐付け済みの場合はスキップ
                if (reviewImageRepository.existsByImageUrlAndReview(imageUrl, review)) {
                    System.out.println("[SKIP] このレビューには既に紐付け済み: " + imageUrl);
                    continue;
                }

                // 未紐付け（review=null）の同一imageUrl画像を全件取得
                List<ReviewImage> unlinkedImages = reviewImageRepository.findByImageUrlAndReviewIsNull(imageUrl);

                if (!unlinkedImages.isEmpty()) {
                    // 一番古い画像1件だけレビューに紐付け
                    ReviewImage imageToBind = unlinkedImages.get(0);
                    imageToBind.setReview(review);
                    reviewImageRepository.save(imageToBind);

                    // 残りの未紐付け画像（ゴミデータ）は物理削除
                    for (int i = 1; i < unlinkedImages.size(); i++) {
                        reviewImageRepository.delete(unlinkedImages.get(i));
                        System.out.println("[DELETE] 重複未紐付け画像を自動削除: id=" + unlinkedImages.get(i).getId());
                    }
                    System.out.println("[LINKED] 画像をレビューに再紐付け完了: " + imageUrl + " -> reviewId: " + review.getReviewId());
                } else {
                    // 未紐付け画像が存在しない場合は何もしない
                    System.out.println("[SKIP] 未紐付け画像が存在しない: " + imageUrl + "（新規登録は禁止）");
                }
            }
        }
        return ResponseEntity.ok(new ApiResponseDto("success"));
    }

    /**
     * レビュー削除（Controllerで認証済userIdのみ受け取る）
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
     * レビュー詳細取得（Controllerで認証済userIdのみ受け取る）
     */
    @Override
    public ResponseEntity<ReviewDetailResponseDto> getReviewDetail(Long reviewId, String currentUserId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("レビューが存在しません"));
        String reviewAuthorId = review.getUser().getUserId();
        boolean isOwner = currentUserId != null && currentUserId.equals(reviewAuthorId);
        boolean isLiked = false;
        if (currentUserId != null) {
            isLiked = reviewLikeRepository.existsByUserUserIdAndReviewReviewId(currentUserId, reviewId);
        }
        ReviewDetailResponseDto detail = ReviewDetailResponseDto.builder()
                .title(review.getTitle())
                .comment(review.getComment())
                .rating(review.getRating())
                .imageList(review.getImageList())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .like(isLiked)
                .likeCount(reviewLikeRepository.countByReviewReviewId(reviewId))
                .userId(reviewAuthorId)
                .isOwner(isOwner)
                .build();
        return ResponseEntity.ok(detail);
    }
}
