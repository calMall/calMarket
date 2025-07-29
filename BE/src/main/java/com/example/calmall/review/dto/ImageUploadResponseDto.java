package com.example.calmall.review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * アップロード成功時に返却される画像URLのレスポンスDTO
 */
@Getter
@AllArgsConstructor
public class ImageUploadResponseDto {
    private String message;           // 成功・失敗メッセージ
    private List<String> imageUrls;   // アップロードされた画像URLリスト
}
