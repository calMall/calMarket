package com.example.calmall.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
<<<<<<< HEAD
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
=======
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * ユーザーログイン用リクエストDTO
 */
@Getter
public class UserLoginRequestDto {

    @NotBlank(message = "email は必須です")
    @Email(message = "email の形式が正しくありません")
    @Size(max = 128, message = "email は128文字以内で入力してください")
    private String email;

    @NotBlank(message = "password は必須です")
    private String password;
>>>>>>> BE
}