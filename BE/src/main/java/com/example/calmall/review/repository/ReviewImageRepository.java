package com.example.calmall.review.repository;

import com.example.calmall.review.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

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
}
