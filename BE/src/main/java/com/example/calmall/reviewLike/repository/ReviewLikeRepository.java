
package com.example.calmall.reviewLike.repository;

import com.example.calmall.reviewLike.entity.ReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;


public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

    // 指定ユーザーが指定レビューにいいねしているか判定
    // userId の型を Long から String に変更
    boolean existsByUserUserIdAndReviewReviewId(String userId, Long reviewId);

    // 指定レビューに対するいいねの件数を取得
    long countByReviewReviewId(Long reviewId);

    // 指定ユーザー・レビューの組み合わせでいいねを削除する
    void deleteByUserUserIdAndReviewReviewId(String userId, Long reviewId);

    // 指定レビューに対するいいね一覧取得
    List<ReviewLike> findAllByReviewReviewId(Long reviewId);


    // 指定ユーザーが「いいね」したレビューID一覧を取得
    @Query("SELECT rl.review.reviewId FROM ReviewLike rl WHERE rl.user.userId = :userId")
    List<Long> findReviewIdsLikedByUser(@Param("userId") String userId);

}