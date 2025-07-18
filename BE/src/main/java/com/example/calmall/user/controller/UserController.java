package com.example.calmall.user.controller;

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
public class UserController {

    private final UserService userService;

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
