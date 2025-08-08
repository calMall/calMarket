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
import org.springframework.http.HttpStatus;
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

    // ログインAPI
    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDto> login(@RequestBody @Valid UserLoginRequestDto requestDto,
                                                      HttpServletRequest request) {
        User user = userService.authenticate(requestDto);

        if (user != null) {
            // ✅ セッション作成（true：なければ作成）
            HttpSession session = request.getSession(true);
            session.setAttribute("user", user); // セッションにユーザー情報保存

            // ✅ デバッグログ出力（セッションIDとユーザー名）
            System.out.println("✅ セッションが作成されました");
            System.out.println("🆔 セッションID: " + session.getId());
            System.out.println("👤 ユーザー: " + user.getNickname());

            return ResponseEntity.ok(UserLoginResponseDto.builder()
                    .message("success")
                    .nickname(user.getNickname())
                    .cartItemCount(0)
                    .build());
        } else {
            System.out.println("❌ ログイン失敗：認証エラー");
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


    // 配送先住所追加API
    @PatchMapping("/users/addresses")
    public ResponseEntity<ApiResponseDto> addAddress(
            @RequestBody @Valid UserAddressRequestDto requestDto,
            HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        User user = (User) (session != null ? session.getAttribute("user") : null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDto("ログインが必要です"));
        }

        String userId = user.getUserId();
        return userService.addAddress(userId, requestDto);
    }

    // 配送先住所削除API
    @PostMapping("/users/addresses/delete")
    public ResponseEntity<ApiResponseDto> deleteAddress(
            @RequestBody @Valid UserAddressRequestDto requestDto,
            HttpServletRequest request) {

        // セッションからログインユーザーを取得
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDto("ログインが必要です"));
        }

        // ユーザーIDとリクエストDTOを使って削除処理を呼び出す
        return userService.deleteAddress(user.getUserId(), requestDto);
    }



    // Email重複確認API（クエリパラメータでemailを受け取る）
    @GetMapping("/users/check-email")
    public ResponseEntity<EmailCheckResponseDto> checkEmail(@RequestParam("email") String email) {
        // Email形式が正しいかを検証
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            return ResponseEntity.badRequest().body(
                    EmailCheckResponseDto.builder()
                            .message("fail")
                            .available(false)
                            .build()
            );
        }

        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(
                EmailCheckResponseDto.builder()
                        .message("success")
                        .available(!exists)
                        .build()
        );
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
    // 払い戻しリクエストAPI
    @PostMapping("/refunds")
    public ResponseEntity<RefundResponseDto> refund(
            @RequestBody @Valid RefundRequestDto requestDto,
            HttpServletRequest request) {

        // セッションチェック
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    RefundResponseDto.builder()
                            .message("fail")
                            .coupons(null)
                            .build()
            );
        }

        // ロジック呼び出し
        return userService.refund(requestDto);
    }
}