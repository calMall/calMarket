package com.example.calmall.review.repository;

import com.example.calmall.review.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    /**
     * 指定された画像URLに一致するレビュー画像のレコードを削除する
     * @param imageUrl 対象画像のURL
     * @return 削除件数（1件なら1、見つからなければ0）
     */
    int deleteByImageUrl(String imageUrl);
}
