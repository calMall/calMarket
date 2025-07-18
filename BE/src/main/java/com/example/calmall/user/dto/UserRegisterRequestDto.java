package com.example.calmall.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * ユーザー新規登録用リクエストDTO
 */
@Getter
@NoArgsConstructor
public class UserRegisterRequestDto {

    @NotBlank(message = "email は必須です")
    @Email(message = "email の形式が正しくありません")
    @Size(max = 128, message = "email は128文字以内で入力してください")
    private String email;

    @NotBlank(message = "password は必須です")
    @Size(min = 8, max = 64, message = "password は8〜64文字で入力してください")
    private String password;

    @NotBlank(message = "nickname は必須です")
    @Size(max = 10, message = "nickname は10文字以内で入力してください")
    private String nickname;

    /**
     * 生年月日（フロントが送らなければ null になる）
     */
    private LocalDate birth;
}
