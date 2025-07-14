package com.example.calmall.repository;

import com.example.calmall.entity.ReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;

// いいね（レビューライク）テーブルの操作を行うリポジトリ
public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {}
