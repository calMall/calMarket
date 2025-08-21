package com.example.calmall.user.dto;

import lombok.AllArgsConstructor;
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
