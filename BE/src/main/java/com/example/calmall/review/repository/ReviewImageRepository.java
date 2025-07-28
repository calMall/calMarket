package com.example.calmall.review.repository;

import com.example.calmall.review.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    /**
     * 画像URLで該当するレコードを削除する
     */
    void deleteByImageUrl(String imageUrl);
}
