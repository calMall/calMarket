package com.example.calmall.user.controller;

import com.example.calmall.user.dto.*;
import com.example.calmall.user.service.UserService;
import com.example.calmall.global.dto.ApiResponseDto;



import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

/**
 * ユーザー関連のAPIを管理するコントローラークラス
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 会員登録（新規登録）
     * @param requestDto ユーザー登録用DTO
     * @return 成功または失敗のレスポンス
     */
    @PostMapping("/users")
    public ApiResponseDto registerUser(@Valid @RequestBody UserRegisterRequestDto requestDto) {
        return userService.registerUser(requestDto);
    }

    /**
     * ログイン処理（セッション保存）
     * @param requestDto ログイン用DTO（メールとパスワード）
     * @param session HttpSession（Springが自動注入）
     * @return ログイン結果とユーザー情報
     */
    @PostMapping("/login")
    public UserLoginResponseDto loginUser(
            @Valid @RequestBody UserLoginRequestDto requestDto,
            HttpSession session
    ) {
        return userService.loginUser(requestDto, session);
    }

    /**
     * ログアウト処理（セッション削除）
     * @param requestDto ニックネーム指定のDTO
     * @param session HttpSession（Springが自動注入）
     * @return 成功または失敗レスポンス
     */
    @PostMapping("/logout")
    public ApiResponseDto logoutUser(
            @RequestBody UserLogoutRequestDto requestDto,
            HttpSession session
    ) {
        return userService.logoutUser(requestDto, session);
    }

    /**
     * ログイン中ユーザーの詳細情報を取得
     * @param session 現在のセッション
     * @return ユーザーのポイント、注文履歴、レビュー履歴
     */
    @GetMapping("/users/me")
    public UserDetailResponseDto getUserDetails(HttpSession session) {
        return userService.getUserDetails(session);
    }

    /**
     * 配送先住所を1件追加
     * @param requestDto 住所追加用DTO
     * @param session ログイン中のセッション
     * @return 成功または失敗レスポンス
     */
    @PatchMapping("/users/addresses")
    public ApiResponseDto addDeliveryAddress(
            @Valid @RequestBody UserAddressRequestDto requestDto,
            HttpSession session
    ) {
        return userService.addDeliveryAddress(requestDto, session);
    }
}