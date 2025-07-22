package com.example.calmall.user.dto;

import jakarta.validation.constraints.NotBlank;
<<<<<<< HEAD
import lombok.Data;
=======
import lombok.Getter;
>>>>>>> BE

/**
 * ログアウト用リクエストDTO
 */
<<<<<<< HEAD
@Data
public class UserLogoutRequestDto {

    @NotBlank(message = "ニックネームは必須です。")
    private String nickname;  // ログアウト対象のユーザーのニックネーム
}
=======
@Getter
public class UserLogoutRequestDto {

    @NotBlank(message = "nickname は必須です")
    private String nickname;
}
>>>>>>> BE
