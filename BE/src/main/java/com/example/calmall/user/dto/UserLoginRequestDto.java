package com.example.calmall.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ログイン用リクエストDTO
 */
@Data
public class UserLoginRequestDto {

    @NotBlank(message = "メールアドレスは必須です。")
    @Email(message = "有効なメールアドレスを入力してください。")
    private String email;  // メールアドレス

    @NotBlank(message = "パスワードは必須です。")
    private String password;  // パスワード（プレーンテキストで送信、サーバー側で検証）
}