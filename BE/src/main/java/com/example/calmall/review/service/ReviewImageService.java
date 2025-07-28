package com.example.calmall.review.service;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.review.dto.ImageUploadResponseDto;
import com.example.calmall.review.dto.ImageDeleteRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 画像アップロードおよび削除処理のインターフェース
 */
public interface ReviewImageService {

    ResponseEntity<ImageUploadResponseDto> uploadImages(List<MultipartFile> files);

    ResponseEntity<ApiResponseDto> deleteImages(ImageDeleteRequestDto requestDto);
}
