
package com.example.calmall.reviewLike.repository;

import com.example.calmall.reviewLike.entity.ReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

    // 指定ユーザーが指定レビューに「いいね」しているか判定
    // userId の型を Long から String に変更
    boolean existsByUserUserIdAndReviewReviewId(String userId, Long reviewId);

    // 指定レビューに対する「いいね」の件数を取得
    long countByReviewReviewId(Long reviewId);

    // 指定ユーザー・レビューの組み合わせで「いいね」を削除する
    void deleteByUserUserIdAndReviewReviewId(String userId, Long reviewId);

    // 指定レビューに対する「いいね」一覧取得
    List<ReviewLike> findAllByReviewReviewId(Long reviewId);
}