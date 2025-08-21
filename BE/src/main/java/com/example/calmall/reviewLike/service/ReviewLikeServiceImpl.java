package com.example.calmall.reviewLike.service;

import com.example.calmall.review.entity.Review;
import com.example.calmall.review.repository.ReviewRepository;
import com.example.calmall.reviewLike.dto.ReviewLikeListResponseDto;
import com.example.calmall.reviewLike.entity.ReviewLike;
import com.example.calmall.reviewLike.repository.ReviewLikeRepository;
import com.example.calmall.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


// レビューの「いいね」機能のビジネスロジックを実装するサービスクラス
@Service
@RequiredArgsConstructor
public class ReviewLikeServiceImpl implements ReviewLikeService {

    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewRepository reviewRepository;

    // いいねのトグル処理を行う
    @Override
    @Transactional
    public boolean toggleLike(User user, Long reviewId) {
        // 対象レビューを取得（存在しない場合は失敗）
        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            return false;
        }

        // 既にいいねしているかを確認
        boolean alreadyLiked = reviewLikeRepository.existsByUserUserIdAndReviewReviewId(user.getUserId(), reviewId);

        if (alreadyLiked) {
            // 既にいいねしている場合は削除
            reviewLikeRepository.deleteByUserUserIdAndReviewReviewId(user.getUserId(), reviewId);
        } else {
            // いいねしていない場合は新規登録
            ReviewLike like = ReviewLike.builder()
                    .user(user)
                    .review(review)
                    .build();
            reviewLikeRepository.save(like);
        }

        return true;
    }

    //  指定レビューに「いいね」したユーザーの一覧を返す
    @Override
    public List<ReviewLikeListResponseDto.LikeUser> getLikesByReviewId(Long reviewId) {
        List<ReviewLike> likes = reviewLikeRepository.findAllByReviewReviewId(reviewId);
        List<ReviewLikeListResponseDto.LikeUser> result = new ArrayList<>();

        for (ReviewLike like : likes) {
            result.add(new ReviewLikeListResponseDto.LikeUser(
                    like.getUser().getId(),
                    like.getUser().getNickname()
            ));
        }

        return result;
    }
}
