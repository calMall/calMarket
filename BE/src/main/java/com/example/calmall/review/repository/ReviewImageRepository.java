package com.example.calmall.review.repository;

import com.example.calmall.review.entity.Review;
import com.example.calmall.review.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ReviewImage（レビュー画像）に対するDB操作を提供するリポジトリインターフェース
 */
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    // 画像URL完全一致で1件削除
    int deleteByImageUrl(String imageUrl);

    // 画像URL完全一致で全件取得（同一URLの重複がある場合にも対応）
    List<ReviewImage> findAllByImageUrl(String imageUrl);

    // 指定URL＋未関連付け画像のみ全件取得（未紐付けの全てを取得したい時用）
    List<ReviewImage> findByImageUrlAndReviewIsNull(String imageUrl);

    // 未関連付けの画像で、指定日時より前のもの
    List<ReviewImage> findByReviewIsNullAndCreatedAtBefore(LocalDateTime cutoff);

    // 指定URL＋未関連付け画像のうち最新1件
    Optional<ReviewImage> findTopByImageUrlAndReviewIsNullOrderByCreatedAtDesc(String imageUrl);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ri FROM ReviewImage ri WHERE ri.imageUrl = :imageUrl AND ri.review IS NULL ORDER BY ri.createdAt ASC")
    Optional<ReviewImage> findTopUnlinkedImageForUpdate(@Param("imageUrl") String imageUrl);

    // imageUrlで1件取得（DB上URLはユニーク保証しないのでOptional）
    Optional<ReviewImage> findByImageUrl(String imageUrl);

    boolean existsByImageUrlAndReview(String imageUrl, Review review);

    @Modifying
    @Query("UPDATE ReviewImage ri SET ri.review = :review WHERE ri.id = :id")
    void updateReviewBinding(@Param("id") Long id, @Param("review") Review review);


}