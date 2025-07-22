package com.example.calmall.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Email 重複確認APIのレスポンスDTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EmailCheckResponseDto {

    /** 結果メッセージ（success または fail） */
    private String message;

    /** 使用可能かどうか（true = 未使用, false = すでに登録されている） */
    private boolean available;
}
