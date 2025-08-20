package com.example.calmall.review.controller;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.review.dto.ImageUploadResponseDto;
import com.example.calmall.review.dto.ImageDeleteRequestDto;
import com.example.calmall.review.service.ReviewImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * レビュー画像のアップロード・削除に関するAPIコントローラー
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews/images")
public class ReviewImageController {

    private final ReviewImageService reviewImageService;


    // 複数画像アップロードAPI（最大3枚）
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImages(@RequestParam("files") List<MultipartFile> files) {
        System.err.println("==== [DEBUG] /upload called, files=" + (files != null ? files.size() : "null"));
        if (files.size() > 3) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto("画像は最大3枚までアップロードできます"));
        }

        return reviewImageService.uploadImages(files);
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponseDto> deleteImages(@Valid @RequestBody ImageDeleteRequestDto requestDto) {
        System.err.println("==== [DEBUG] /delete called, body=" + (requestDto != null ? requestDto.getImageUrls() : "null"));
        return reviewImageService.deleteImages(requestDto);
    }

}
