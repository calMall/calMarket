package com.example.calmall.review.repository;

import com.example.calmall.review.entity.Review;
import com.example.calmall.review.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * ReviewImage（レビュー画像）に対するDB操作を提供するリポジトリインターフェース
 */
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    /** imageUrl で 1 件取得 */
    Optional<ReviewImage> findByImageUrl(String imageUrl);

    /** imageUrl 完全一致で 1 件削除（削除件数を返す） */
    int deleteByImageUrl(String imageUrl);

    /** 指定レビューに紐付いている全画像を取得 */
    List<ReviewImage> findAllByReview(Review review);

    /** Native SQL で画像をレビューに紐付け（INSERT 回避） */
    @Modifying
    @Query(value = "UPDATE review_images SET review_id = :reviewId WHERE id = :id", nativeQuery = true)
    void updateReviewBindingNative(@Param("id") Long id, @Param("reviewId") Long reviewId);

    /** 複数画像の紐付け解除（review_id を NULL にする） */
    @Modifying
    @Query(value = "UPDATE review_images SET review_id = NULL WHERE review_id = :reviewId AND image_url IN :urls", nativeQuery = true)
    void unbindImagesFromReview(@Param("reviewId") Long reviewId, @Param("urls") List<String> urls);
}
