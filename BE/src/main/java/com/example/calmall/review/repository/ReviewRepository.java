package com.example.calmall.review.repository;

import com.example.calmall.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Reviewエンティティに対するDBアクセス処理を定義するリポジトリインターフェース
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 商品ごとのレビューをページング付きで取得
    Page<Review> findByProduct_ItemCode(String itemCode, Pageable pageable);

    // 商品ごとの全レビューを取得（ページングなし）
    List<Review> findByProduct_ItemCode(String itemCode);

    // ユーザーのレビュー一覧を取得（ページング付き）
    Page<Review> findByUser_UserId(String userId, Pageable pageable);

    // 商品とユーザーを指定してレビューを取得（マイレビュー用）
    Optional<Review> findByProduct_ItemCodeAndUser_UserId(String itemCode, String userId);

    // 指定されたユーザーIDと商品コードに一致し、かつ削除済みのレビューが存在するかを検索
    Optional<Review> findByUser_UserIdAndProduct_ItemCodeAndDeletedIsTrue(String userId, String itemCode);

    // 商品・ユーザー・作成日時から削除済みレビューを検索（再投稿制限用）
    Optional<Review> findByProduct_ItemCodeAndUser_UserIdAndDeletedTrue(String itemCode, String userId);
}
