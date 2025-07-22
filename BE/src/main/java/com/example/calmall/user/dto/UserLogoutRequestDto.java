package com.example.calmall.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * ログアウト用リクエストDTO
 */
@Getter
public class UserLogoutRequestDto {

    @NotBlank(message = "nickname は必須です")
    private String nickname;
}
