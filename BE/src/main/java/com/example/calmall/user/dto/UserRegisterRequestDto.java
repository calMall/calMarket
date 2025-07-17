package com.example.calmall.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ユーザー登録用リクエストDTO
 */
@Data
public class UserRegisterRequestDto {

    @NotBlank(message = "メールアドレスは必須です。")
    @Email(message = "有効なメールアドレスを入力してください。")
    private String email;    // メールアドレス

    @NotBlank(message = "パスワードは必須です。")
    private String password; // パスワード（ハッシュ化して保存すること）

    @NotBlank(message = "ニックネームは必須です。")
    private String nickname; // ニックネーム
}
