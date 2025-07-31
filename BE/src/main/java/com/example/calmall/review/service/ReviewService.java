package com.example.calmall.review.service;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.review.dto.*;
import org.springframework.http.ResponseEntity;

/**
 * レビュー機能に関するサービスインターフェース
 * - 投稿、取得（商品・ユーザー別）、編集、削除を提供
 */
public interface ReviewService {

    /**
     * レビューを投稿する
     * - セッションから取得した userId を使用してレビューを登録する
     * - 購入後1ヶ月以内であることが条件
     * - 同一商品に対しては1回のみ投稿可能（削除済も不可）
     *
     * @param requestDto レビュー投稿リクエストデータ
     * @param userId セッションから取得したユーザーID
     * @return 投稿成功またはエラーメッセージを含むレスポンス
     */
    ResponseEntity<ApiResponseDto> postReview(ReviewRequestDto requestDto, String userId);

    /**
     * 商品に紐づくレビュー一覧を取得する
     * - ページネーション対応
     * - 評価統計（★ごとの件数）
     * - マイレビュー（自分が投稿していれば含まれる）
     * - 各レビューに対する「いいね」情報も含む
     *
     * @param itemCode 商品コード
     * @param userId セッションのユーザーID（null可：未ログイン対応）
     * @param page ページ番号（0から開始）
     * @param size ページサイズ
     * @return レビュー一覧、統計、マイレビューを含むレスポンス
     */
    ResponseEntity<ReviewListByItemResponseDto> getReviewsByItem(String itemCode, String userId, int page, int size);

    /**
     * ユーザーが投稿したレビュー一覧を取得する
     * - ページネーション対応
     *
     * @param userId ユーザーID（セッションから取得）
     * @param page ページ番号（0から開始）
     * @param size ページサイズ
     * @return レビュー一覧レスポンス
     */
    ResponseEntity<ReviewListByUserResponseDto> getReviewsByUser(String userId, int page, int size);

    /**
     * レビューを編集する
     * - 対象レビューが本人の投稿である必要がある
     *
     * @param reviewId 編集対象のレビューID
     * @param requestDto 編集内容（評価・タイトル・コメント・画像）
     * @param userId セッションのユーザーID
     * @return 編集成功または失敗メッセージを含むレスポンス
     */
    ResponseEntity<ApiResponseDto> updateReview(Long reviewId, ReviewUpdateRequestDto requestDto, String userId);

    /**
     * レビューを削除する（論理削除）
     * - 対象レビューが本人の投稿である必要がある
     *
     * @param reviewId 削除対象のレビューID
     * @param userId セッションのユーザーID
     * @return 削除成功または失敗メッセージを含むレスポンス
     */
    ResponseEntity<ApiResponseDto> deleteReview(Long reviewId, String userId);

    /**
     * レビュー詳細を取得する
     * @param reviewId 対象レビューのID
     * @param currentUserId 現在ログイン中のユーザーID（未ログイン時は null）
     * @return レビュー詳細情報
     */
    ResponseEntity<ReviewDetailResponseDto> getReviewDetail(Long reviewId, String currentUserId);
}
