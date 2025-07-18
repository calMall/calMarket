package com.example.calmall.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * ユーザーの配送先住所追加リクエストDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressRequestDto {

    // ユーザーID（UUIDベース）
    @NotBlank(message = "ユーザーIDは必須です")
    private String userId;

    // 郵便番号（例：123-4567）
    @NotBlank(message = "郵便番号は必須です")
    @Pattern(regexp = "^\\d{3}-\\d{4}$", message = "郵便番号の形式が正しくありません（例：123-4567）")
    private String postalCode;

    // 都道府県市区町など
    @NotBlank(message = "市区町村・番地は必須です")
    @Size(max = 100, message = "address1 は100文字以内で入力してください")
    private String address1;

    // 建物名部屋番号など
    @NotBlank(message = "建物名・部屋番号は必須です")
    @Size(max = 100, message = "address2 は100文字以内で入力してください")
    private String address2;

}
