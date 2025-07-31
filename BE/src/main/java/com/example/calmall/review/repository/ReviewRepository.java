package com.example.calmall.review.repository;

import com.example.calmall.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Reviewエンティティに対するDBアクセス処理を定義するリポジトリインターフェース
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 対象商品のレビューをページング付きで取得
    Page<Review> findByProduct_ItemCode(String itemCode, Pageable pageable);

    // 対象商品の全レビューを取得（ページングなし）
    List<Review> findByProduct_ItemCode(String itemCode);

    // 対象ユーザーのレビューをページング付きで取得
    Page<Review> findByUser_UserId(String userId, Pageable pageable);

    // 指定商品・ユーザーのレビュー（マイレビュー）を取得
    Optional<Review> findByProduct_ItemCodeAndUser_UserId(String itemCode, String userId);

    // 指定ユーザー・商品で削除済みレビューがあるか（再投稿制限用）
    Optional<Review> findByUser_UserIdAndProduct_ItemCodeAndDeletedIsTrue(String userId, String itemCode);

    // 商品・ユーザー・作成日時から削除済みレビューを検索（再投稿制限用）
    Optional<Review> findByProduct_ItemCodeAndUser_UserIdAndDeletedTrue(String itemCode, String userId);

    // 指定商品のレビュー件数を取得（Product詳細取得で使用）
    int countByProduct_ItemCode(String itemCode);

    // 指定商品の評価平均（score）を取得（Product詳細取得で使用）
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.itemCode = :itemCode")
    Double findAverageRatingByItemCode(@Param("itemCode") String itemCode);
}
