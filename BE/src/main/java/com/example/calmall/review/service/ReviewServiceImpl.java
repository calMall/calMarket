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
     * - 画像は「未紐付け状態（review=null）」のみ紐付け
     * - 他レビューに紐付いている画像はスキップ（エラーにしない）
     * - Review.imageList には「実際に紐付けに成功した画像のみ」を格納
     * - ★ INSERT を絶対発生させず、UPDATE のみ行う
     */
    @Override
    @Transactional
    public ResponseEntity<ApiResponseDto> postReview(ReviewRequestDto requestDto, String userId) {

        // ===== ユーザー存在チェック =====
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが存在しません"));

        // ===== 商品存在チェック =====
        Product product = productRepository.findByItemCode(requestDto.getItemCode())
                .orElseThrow(() -> new IllegalArgumentException("商品が存在しません"));

        // ===== 再投稿防止チェック（削除済みレビューあり） =====
        if (!reviewRepository.findByUser_UserIdAndProduct_ItemCodeAndDeletedTrue(userId, product.getItemCode()).isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("削除済レビューが存在するため再投稿できません"));
        }

        // ===== 再投稿防止チェック（既に投稿済み） =====
        if (!reviewRepository.findByProduct_ItemCodeAndUser_UserIdAndDeletedFalse(product.getItemCode(), userId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("この商品には既にレビューを投稿済みです"));
        }

        // ===== 購入履歴チェック =====
        List<Orders> orders = ordersRepository.findOrdersByUserAndItemCode(userId, product.getItemCode());
        if (orders.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("未購入の商品にはレビューできません"));
        }

        // ===== 購入1ヶ月以内チェック =====
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        boolean purchasedWithinOneMonth =
                ordersRepository.existsPurchaseWithinPeriod(userId, product.getItemCode(), oneMonthAgo);
        if (!purchasedWithinOneMonth) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("購入後1ヶ月以内のユーザーのみレビュー可能です"));
        }

        // ===== レビュー作成（画像は後でセット） =====
        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(requestDto.getRating())
                .title(requestDto.getTitle())
                .comment(requestDto.getComment())
                .imageList(new ArrayList<>()) // 表示用のみ、DBには保存しない
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleted(false)
                .build();

        Review savedReview = reviewRepository.save(review);

        // ===== 紐付け成功した画像URLだけを保持（重複除去済み） =====
        Set<String> finalImageList = new LinkedHashSet<>();

        if (requestDto.getImageList() != null && !requestDto.getImageList().isEmpty()) {

            // 1リクエスト内の重複URLを除去
            Set<String> uniqueUrls = new LinkedHashSet<>(requestDto.getImageList());

            for (String imageUrl : uniqueUrls) {
                Optional<ReviewImage> optionalReviewImage = reviewImageRepository.findByImageUrl(imageUrl);

                // DBに存在しない画像はスキップ
                if (optionalReviewImage.isEmpty()) {
                    System.out.println("[SKIP] DBに存在しない画像: " + imageUrl);
                    continue;
                }

                ReviewImage reviewImage = optionalReviewImage.get();

                // 他のレビューに紐付いている場合はスキップ
                if (reviewImage.getReview() != null &&
                        !reviewImage.getReview().getReviewId().equals(savedReview.getReviewId())) {
                    System.out.println("[SKIP] 他のレビューに紐付け済: " + imageUrl);
                    continue;
                }

                // このレビューに既に紐付いている場合もスキップ
                if (reviewImage.getReview() != null &&
                        reviewImage.getReview().getReviewId().equals(savedReview.getReviewId())) {
                    System.out.println("[SKIP] このレビューに既に紐付け済: " + imageUrl);
                    continue;
                }

                // ===== Hibernate の INSERT を完全防止し、ネイティブ SQL で外部キーだけ更新 =====
                reviewImageRepository.updateReviewBindingNative(reviewImage.getId(), savedReview.getReviewId());

                // 表示用URLリストに追加
                finalImageList.add(imageUrl);
                System.out.println("[LINKED] 紐付け完了: " + imageUrl);
            }
        }

        // ===== 紐付け成功したURLのみ Review.imageList にセット（DBには保存されない） =====
        savedReview.setImageList(new ArrayList<>(finalImageList));

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
     * レビュー編集処理
     * - 本人のみ編集可能
     * - 評価・タイトル・コメントを更新
     * - 画像は「削除」「追加」「維持」を正しく処理
     * - review_images テーブルの外部キーのみ更新（INSERT は発生しない）
     * */
    @Override
    @Transactional
    public ResponseEntity<ReviewDetailResponseDto> updateReview(Long reviewId, ReviewUpdateRequestDto requestDto, String userId) {

        // ===== 1. レビュー存在チェック =====
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("レビューが存在しません"));

        // ===== 2. 論理削除チェック =====
        if (review.isDeleted()) {
            throw new IllegalArgumentException("削除済みレビューは編集できません");
        }

        // ===== 3. 本人確認 =====
        if (!review.getUser().getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // ===== 4. 評価・タイトル・コメントを更新 =====
        review.setRating(requestDto.getRating());
        review.setTitle(requestDto.getTitle());
        review.setComment(requestDto.getComment());
        review.setUpdatedAt(LocalDateTime.now());

        // ===== 5. 既存の画像枚数を取得 =====
        List<String> currentImages = reviewImageRepository.findAllByReview(review).stream()
                .map(ReviewImage::getImageUrl)
                .collect(Collectors.toList());

        int currentCount = currentImages.size();

        // ===== 6. 新規追加画像のURLリスト（重複除去） =====
        List<String> addUrls = requestDto.getImageList() != null
                ? new ArrayList<>(new LinkedHashSet<>(requestDto.getImageList()))
                : new ArrayList<>();

        // ===== 7. 追加枚数チェック（純追加モード） =====
        if (currentCount + addUrls.size() > 3) {
            throw new IllegalArgumentException("画像は最大3枚まで追加できます（超える場合は削除APIで削除してください）");
        }

        // ===== 8. 新しい画像だけを紐付け =====
        for (String imageUrl : addUrls) {
            Optional<ReviewImage> optionalImage = reviewImageRepository.findByImageUrl(imageUrl);

            // DBに存在しない → スキップ
            if (optionalImage.isEmpty()) continue;

            ReviewImage img = optionalImage.get();

            // 他レビューに紐付いている → スキップ
            if (img.getReview() != null && !img.getReview().getReviewId().equals(reviewId)) continue;

            // 紐付け更新（INSERT発生なし）
            reviewImageRepository.updateReviewBindingNative(img.getId(), reviewId);
        }

        // ===== 9. 最新の画像リストを取得 =====
        List<String> finalImageList = reviewImageRepository.findAllByReview(review).stream()
                .map(ReviewImage::getImageUrl)
                .collect(Collectors.toList());

        review.setImageList(finalImageList);
        reviewRepository.save(review);

        // ===== 10. 編集後の詳細DTOを作成して返す =====
        boolean isLiked = reviewLikeRepository.existsByUserUserIdAndReviewReviewId(userId, reviewId);

        ReviewDetailResponseDto responseDto = ReviewDetailResponseDto.builder()
                .userId(review.getUser().getUserId())
                .title(review.getTitle())
                .comment(review.getComment())
                .rating(review.getRating())
                .imageList(finalImageList)
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .like(isLiked)
                .likeCount(reviewLikeRepository.countByReviewReviewId(reviewId))
                .isOwner(true)
                .build();

        return ResponseEntity.ok(responseDto);
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
