package com.example.calmall.user.dto;

import lombok.AllArgsConstructor;
<<<<<<< HEAD
import lombok.Data;

/**
 * ログイン成功時のレスポンスDTO
 */
@Data
@AllArgsConstructor
public class UserLoginResponseDto {

    private String message;         // 成功時は "success"、失敗時は "fail"
    private int cartItemCount;     // カート内の商品数
    private String nickname;       // ユーザーのニックネーム
}
=======
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ログインAPIのレスポンスDTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginResponseDto {

    // 処理結果（"success" または "fail"）
    private String message;

    // カート内の商品数
    private int cartItemCount;

    // ユーザーのニックネーム
    private String nickname;
}
>>>>>>> BE
