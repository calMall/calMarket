package com.example.calmall.review.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.Getter;

import java.util.List;


// 削除したい画像のリクエストDTO
@Data
public class ImageDeleteRequestDto {
    @NotEmpty(message = "削除する画像URLを1つ以上指定してください")
    private List<String> imageUrls;
}
