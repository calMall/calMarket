package com.example.calmall.review.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

/**
 * 削除したい画像のURLをリストで送信するためのリクエストDTO
 */
@Getter
public class ImageDeleteRequestDto {

    @NotEmpty(message = "削除する画像URLを1つ以上指定してください")
    private List<String> imageUrls;  // 削除対象の画像URL
}
