package com.example.calmall.review.repository;

import com.example.calmall.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

// レビューテーブルの操作を行うリポジトリ
public interface ReviewRepository extends JpaRepository<Review, Long> {}
