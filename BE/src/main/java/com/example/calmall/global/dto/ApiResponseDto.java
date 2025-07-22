package com.example.calmall.global.dto;

<<<<<<< HEAD
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
=======
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
>>>>>>> BE
