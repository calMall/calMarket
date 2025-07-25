package com.example.calmall.reviewLike.service;

import com.example.calmall.reviewLike.dto.ReviewLikeListResponseDto;
import com.example.calmall.user.entity.User;

import java.util.List;

/**
 * レビューの「いいね」機能に関するビジネスロジックを定義するサービスインターフェース
 */
public interface ReviewLikeService {

    /**
     * 指定ユーザーが指定レビューに対して「いいね」トグルを行う（追加または削除）
     *
     * @param user     現在ログイン中のユーザー
     * @param reviewId 対象レビューID
     * @return トグル操作が成功したかどうか
     */
    boolean toggleLike(User user, Long reviewId);

    /**
     * 指定されたレビューに「いいね」したユーザーの一覧を取得
     *
     * @param reviewId 対象レビューID
     * @return いいねしたユーザーリスト
     */
    List<ReviewLikeListResponseDto.LikeUser> getLikesByReviewId(Long reviewId);
}
