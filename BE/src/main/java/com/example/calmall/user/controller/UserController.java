package com.example.calmall.user.controller;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.user.dto.*;
import com.example.calmall.user.entity.User;
import com.example.calmall.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
                                                      HttpServletRequest request) {
        User user = userService.authenticate(requestDto);

        if (user != null) {
            // ✅ セッションにユーザー情報を保存（これだけでJSESSIONIDクッキーが自動で発行）
            HttpSession session = request.getSession(true);
            session.setAttribute("user", user); // 任意で保持したい情報

            // ✅ レスポンス返却
            return ResponseEntity.ok(UserLoginResponseDto.builder()
                    .message("success")
                    .nickname(user.getNickname())
                    .cartItemCount(0)
                    .build());
        } else {
            return ResponseEntity.status(401).body(
                    UserLoginResponseDto.builder()
                            .message("fail")
                            .build()
            );
        }
    }


    // ログアウトAPI（クッキー削除）
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false); // セッションがあれば取得
        if (session != null) {
            session.invalidate(); // セッション無効化
        }
        return ResponseEntity.ok(new ApiResponseDto("success"));
    }



    // ユーザー詳細取得API（HeaderからuserId取得）
    @GetMapping("/users/me")
    public ResponseEntity<UserDetailResponseDto> getUserDetail(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            return ResponseEntity.status(401).body(
                    UserDetailResponseDto.builder().message("fail").build());
        }

        User user = (User) session.getAttribute("user");

        // UserServiceの getUserDetail(String userId) を呼び出し
        return userService.getUserDetail(user.getUserId());
    }


    // 配送先住所追加API（DTOにuserIdを含む）
    @PatchMapping("/users/addresses")
    public ResponseEntity<ApiResponseDto> addAddress(@RequestBody @Valid UserAddressRequestDto requestDto) {
        return userService.addAddress(requestDto);
    }

    @GetMapping("/test-session")
    public ResponseEntity<String> testSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.ok("❌ session 不存在");
        }

        Object user = session.getAttribute("user");
        return ResponseEntity.ok("✅ session 存在，user: " + (user != null ? user.toString() : "null"));
    }

}
