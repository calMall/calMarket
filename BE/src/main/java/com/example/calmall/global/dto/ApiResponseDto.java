package com.example.calmall.global.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
/**
 * 全API共通のシンプルなレスポンスDTO
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponseDto {
    private String message; // "success" または "fail"
}
