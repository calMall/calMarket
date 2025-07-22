package com.example.calmall.user.controller;

<<<<<<< HEAD
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
=======
import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.user.dto.*;
import com.example.calmall.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ユーザー関連のAPIを提供するコントローラークラス
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
>>>>>>> BE
public class UserController {

    private final UserService userService;

<<<<<<< HEAD
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
=======
    // ユーザー新規登録API
    @PostMapping("/users")
    public ResponseEntity<ApiResponseDto> register(@RequestBody @Valid UserRegisterRequestDto requestDto) {
        return userService.register(requestDto);
    }

    // ログインAPI（成功時 HttpOnlyクッキーをセット）
    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDto> login(@RequestBody @Valid UserLoginRequestDto requestDto,
                                                      HttpServletResponse response) {
        UserLoginResponseDto result = userService.login(requestDto);

        if ("success".equals(result.getMessage())) {
            Cookie cookie = new Cookie("token", "ダミートークン");
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            response.addCookie(cookie);
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(401).body(result);
        }
    }

    // ログアウトAPI（クッキー削除）
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto> logout(@RequestBody(required = false) UserLogoutRequestDto requestDto,
                                                 HttpServletResponse response) {
        Cookie cookie = new Cookie("token", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        return userService.logout(requestDto);
    }


    // ユーザー詳細取得API（HeaderからuserId取得）
    @GetMapping("/users/me")
    public ResponseEntity<UserDetailResponseDto> getUserDetail(@RequestHeader("userId") String userId) {
        return userService.getUserDetail(userId);
    }

    // 配送先住所追加API（DTOにuserIdを含む）
    @PatchMapping("/users/addresses")
    public ResponseEntity<ApiResponseDto> addAddress(@RequestBody @Valid UserAddressRequestDto requestDto) {
        return userService.addAddress(requestDto);
    }
}
>>>>>>> BE
