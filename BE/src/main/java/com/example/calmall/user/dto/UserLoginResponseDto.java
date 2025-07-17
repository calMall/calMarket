package com.example.calmall.user.dto;

import lombok.AllArgsConstructor;
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