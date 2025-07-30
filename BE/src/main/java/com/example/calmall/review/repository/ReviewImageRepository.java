package com.example.calmall.review.repository;

import com.example.calmall.review.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ReviewImage（レビュー画像）に対するDB操作を提供するリポジトリインターフェース。
 */
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    /**
     * 指定された画像URLに一致するレビュー画像のレコードを削除する
     * @param imageUrl 対象画像のURL
     * @return 削除件数（1件なら1、見つからなければ0）
     */
    int deleteByImageUrl(String imageUrl);

    /**
     * 指定された画像URLに一致する画像をすべて取得する（レビュー紐付けのため）
     * - Optional ではなく List にすることで、同一URLが複数存在しても例外が発生しない
     * @param imageUrl 対象画像のURL
     * @return 一致するReviewImageのリスト（存在しなければ空リスト）
     */
    List<ReviewImage> findAllByImageUrl(String imageUrl);

    /**
     * 指定された画像URLでかつレビューに未関連付けの画像を取得する
     * - レビュー投稿時に既存の未関連付け画像のみを更新するために使用
     * @param imageUrl 対象画像のURL
     * @return レビュー未関連付けのReviewImageリスト
     */
    List<ReviewImage> findByImageUrlAndReviewIsNull(String imageUrl);

    /**
     * レビューに関連付けられていない古い画像を取得する（クリーンアップ用）
     * @param cutoff この日時より前に作成された画像が対象
     * @return レビュー未関連付けの古いReviewImageリスト
     */
    List<ReviewImage> findByReviewIsNullAndCreatedAtBefore(LocalDateTime cutoff);

    /**
     * 指定された画像URLでかつレビューに未関連付けの画像を1件取得（最新順）
     * @param imageUrl 対象画像のURL
     * @return レビュー未関連付けの最新のReviewImage（Optional）
     */
    Optional<ReviewImage> findTopByImageUrlAndReviewIsNullOrderByCreatedAtDesc(String imageUrl);

    /**
     * 高並列環境での重複防止用：指定URLの未関連付け画像を排他ロック付きで取得
     * - これにより同時に複数スレッドが同じ画像を取得・更新することを防止
     * @param imageUrl 対象画像のURL
     * @return レビュー未関連付けの最新のReviewImage（Optional）
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ri FROM ReviewImage ri WHERE ri.imageUrl = :imageUrl AND ri.review IS NULL ORDER BY ri.createdAt DESC")
    Optional<ReviewImage> findTopUnlinkedImageForUpdate(@Param("imageUrl") String imageUrl);
}
