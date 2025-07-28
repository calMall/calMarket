// ファイル: com.example.calmall.review.repository.ReviewImageRepository.java

package com.example.calmall.review.repository;

import com.example.calmall.review.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * ReviewImage エンティティのリポジトリ
 */
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    /**
     * 指定された画像URLのレコードを削除する
     * @param imageUrl 削除対象の画像URL
     * @return 削除されたレコード件数（通常は 0 または 1）
     */
    @Transactional
    int deleteByImageUrl(String imageUrl);
}
