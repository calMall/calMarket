package com.example.calmall.global.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 一般的なAPIレスポンスDTO（成功/失敗のみ）
 */
@Data
@AllArgsConstructor
public class ApiResponseDto {

    private String message; // "success" または "fail"
}