package com.example.calmall.review.repository;

import com.example.calmall.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Reviewエンティティに対するDBアクセス処理を定義するリポジトリインターフェース
 * Spring Data JPAを使用して、レビュー情報を取得・集計する
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 対象商品のレビューをページング付きで取得（削除されていないレビューのみ）
    Page<Review> findByProduct_ItemCodeAndDeletedFalse(String itemCode, Pageable pageable);

    // 対象商品の全レビューを取得（削除されていないレビューのみ、ページングなし）
    List<Review> findByProduct_ItemCodeAndDeletedFalse(String itemCode);

    // 対象ユーザーのレビューをページング付きで取得（削除されていないレビューのみ）
    Page<Review> findByUser_UserIdAndDeletedFalse(String userId, Pageable pageable);

    // 指定商品・ユーザーのレビュー（削除されていない）→ List で返す（Optional だと複数ヒット時に例外発生するため）
    List<Review> findByProduct_ItemCodeAndUser_UserIdAndDeletedFalse(String itemCode, String userId);

    // 削除済みレビュー（再投稿制限用）→ List で返す（複数ヒットを許容）
    List<Review> findByUser_UserIdAndProduct_ItemCodeAndDeletedTrue(String userId, String itemCode);

    // 指定商品の有効（未削除）レビュー件数を取得（Product詳細取得で使用）
    int countByProductItemCodeAndDeletedFalse(String itemCode);

    /**
     * 指定商品の平均評価を取得（削除されていないレビューのみ）
     * - COALESCE を使って null を 0 に置き換える
     */
    @Query("SELECT COALESCE(AVG(r.rating), 0) " +
            "FROM Review r " +
            "WHERE r.product.itemCode = :itemCode AND r.deleted = false")
    Double findAverageRatingByItemCode(@Param("itemCode") String itemCode);
}