package com.example.calmall.ReviewLike;

import org.springframework.data.jpa.repository.JpaRepository;

// いいね（レビューライク）テーブルの操作を行うリポジトリ
public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {}
