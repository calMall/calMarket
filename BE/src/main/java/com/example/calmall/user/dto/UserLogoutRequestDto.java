package com.example.calmall.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ログアウト用リクエストDTO
 */
@Data
public class UserLogoutRequestDto {

    @NotBlank(message = "ニックネームは必須です。")
    private String nickname;  // ログアウト対象のユーザーのニックネーム
}