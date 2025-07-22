package com.example.calmall.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
    @Size(min = 8, max = 20, message = "password は8文字以上20文字以内で入力してください")
    private String password;
}