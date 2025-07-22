package com.example.calmall.user.service;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.user.dto.*;
import com.example.calmall.user.entity.User;
import org.springframework.http.ResponseEntity;

/**
 * ユーザー関連のサービスインターフェース
 * 各メソッドはユーザー情報の登録、認証、ログアウト、詳細取得などの処理を担当する
 */
public interface UserService {

    /**
     * ユーザー新規登録
     *
     * @param requestDto ユーザー登録用のリクエストDTO
     * @return レスポンスメッセージ（success または エラー内容）
     */
    ResponseEntity<ApiResponseDto> register(UserRegisterRequestDto requestDto);

    /**
     * Email がすでに存在するか確認する
     *
     * @param email 確認したいemail
     * @return true：すでに存在する、false：未登録
     */
    boolean existsByEmail(String email);

    /**
     * ユーザー認証処理（セッションはControllerで管理）
     *
     * @param requestDto ログイン用のリクエストDTO
     * @return 認証に成功したユーザー情報、失敗時は null
     */
    User authenticate(UserLoginRequestDto requestDto);

    /**
     * ログアウト処理（セッションの破棄はControllerで実施）
     *
     * @param requestDto ログアウト用のリクエストDTO（未使用可）
     * @return レスポンスメッセージ（success）
     */
    default ResponseEntity<ApiResponseDto> logout(UserLogoutRequestDto requestDto) {
        return ResponseEntity.ok(new ApiResponseDto("success"));
    }


    /**
     * ユーザー詳細情報の取得
     *
     * @param userId ユーザーID
     * @return ユーザーの詳細情報レスポンス
     */
    ResponseEntity<UserDetailResponseDto> getUserDetail(String userId);

    /**
     * 払い戻し（注文キャンセル）の処理
     *
     * @param requestDto 払い戻しリクエストDTO（注文ID含む）
     * @return 払い戻し処理結果（成功または失敗）
     */
    ResponseEntity<RefundResponseDto> refund(RefundRequestDto requestDto);

    /**
     * 配送先住所の追加処理（重複チェックあり）
     *
     * @param requestDto 配送先住所の追加リクエストDTO
     * @return 処理結果（success または fail）
     */
    ResponseEntity<ApiResponseDto> addAddress(String userId, UserAddressRequestDto requestDto);

}
