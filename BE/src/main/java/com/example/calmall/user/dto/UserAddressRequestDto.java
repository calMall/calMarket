package com.example.calmall.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 配送先住所追加用のリクエストDTO
 */
@Data
public class UserAddressRequestDto {

    @NotBlank(message = "郵便番号は必須です。")
    private String postalCode;  // 郵便番号

    @NotBlank(message = "住所（市区町村）は必須です。")
    private String address1;    // 住所1（市区町村・番地など）

    @NotBlank(message = "住所（建物名など）は必須です。")
    private String address2;    // 住所2（建物名や部屋番号など）
}