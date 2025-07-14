// ReviewRepository.java
package com.example.calmall.repository;

import com.example.calmall.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

// レビューテーブルの操作を行うリポジトリ
public interface ReviewRepository extends JpaRepository<Review, Long> {}
