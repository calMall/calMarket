package com.example.calmall.review.repository;

import com.example.calmall.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * レビュー情報を扱うJPAリポジトリ
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 商品コードに紐づくレビュー一覧（ページネーション）
    Page<Review> findByProduct_ItemCode(String itemCode, Pageable pageable);

    // 商品コードに紐づくレビュー一覧（全件：統計用）
    List<Review> findByProduct_ItemCode(String itemCode);

    // 商品コードとユーザーUUIDに一致するレビュー（MyReview取得）
    Optional<Review> findByProduct_ItemCodeAndUser_UserId(String itemCode, String userId);

    // ユーザーUUIDに紐づくレビュー一覧（ページネーション）
    Page<Review> findByUser_UserId(String userId, Pageable pageable);
}
