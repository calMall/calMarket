package com.example.calmall.user.service;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.user.dto.*;
import com.example.calmall.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

/**
 * ユーザー関連のサービスインターフェース
 */
public interface UserService {

    // ユーザー新規登録
    ResponseEntity<ApiResponseDto> register(UserRegisterRequestDto requestDto);

    // Email 重複チェック
    boolean existsByEmail(String email);

    // ログイン認証
    User authenticate(UserLoginRequestDto requestDto);

    // ログアウト
    ResponseEntity<ApiResponseDto> logout(HttpServletRequest request);

    // ユーザー詳細取得
    ResponseEntity<UserDetailResponseDto> getUserDetail(String userId);

    // 配送先住所追加
    ResponseEntity<ApiResponseDto> addAddress(String userId, UserAddressRequestDto requestDto);

    // 配送先住所削除
    ResponseEntity<ApiResponseDto> deleteAddress(String userId, UserAddressRequestDto requestDto);
}
