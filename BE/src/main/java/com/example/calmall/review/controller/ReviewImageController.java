package com.example.calmall.review.controller;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.review.dto.ImageUploadResponseDto;
import com.example.calmall.review.dto.ImageDeleteRequestDto;
import com.example.calmall.review.service.ReviewImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * レビュー画像アップロード・削除を管理するコントローラー
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews/images")
public class ReviewImageController {

    private final ReviewImageService reviewImageService;

    /**
     * 画像アップロードAPI（最大3枚）
     * @param files Multipart形式の画像ファイル（jpg/png）
     * @return アップロード成功メッセージと画像URLリスト
     */
    @PostMapping("/upload")
    public ResponseEntity<ImageUploadResponseDto> uploadImages(
            @RequestParam("files") List<MultipartFile> files) {
        return reviewImageService.uploadImages(files);
    }

    /**
     * アップロード済み画像の削除API
     * @param requestDto 削除したい画像のURLリスト
     * @return 成功・失敗メッセージ
     */
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponseDto> deleteImages(
            @RequestBody @Valid ImageDeleteRequestDto requestDto) {
        return reviewImageService.deleteImages(requestDto);
    }
}
