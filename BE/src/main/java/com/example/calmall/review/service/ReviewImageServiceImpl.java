// ファイルパス: com.example.calmall.review.service.ReviewImageServiceImpl.java

package com.example.calmall.review.service;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.review.dto.ImageUploadResponseDto;
import com.example.calmall.review.dto.ImageDeleteRequestDto;
import com.example.calmall.review.entity.ReviewImage;
import com.example.calmall.review.repository.ReviewImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * アップロード画像をサーバーに保存・削除・DB保存するサービスの実装クラス
 */
@Service
@RequiredArgsConstructor
public class ReviewImageServiceImpl implements ReviewImageService {

    // application.properties からアップロード先パスを取得
    @Value("${file.upload-dir}")
    private String uploadDir;

    // 外部公開用のパスプレフィックス
    private static final String FILE_URL_PREFIX = "/uploads/";

    // DBにアクセスするためのリポジトリ
    private final ReviewImageRepository reviewImageRepository;

    /**
     * 画像ファイルを保存し、URLを返却（最大3枚まで）
     */
    @Override
    public ResponseEntity<ImageUploadResponseDto> uploadImages(List<MultipartFile> files) {
        // アップロード上限チェック
        if (files.size() > 3) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ImageUploadResponseDto("画像は最大3枚までです", List.of()));
        }

        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            String contentType = file.getContentType();

            // JPGまたはPNGのみ許可
            if (!Objects.equals(contentType, "image/jpeg") && !Objects.equals(contentType, "image/png")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ImageUploadResponseDto("JPGまたはPNG形式のみアップロード可能です", List.of()));
            }

            try {
                // 元のファイル名と拡張子を取得
                String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

                // UUID付きファイル名生成
                String filename = UUID.randomUUID() + extension;

                // 保存先のパスを組み立て
                Path uploadPath = Paths.get(uploadDir).resolve(filename);
                Files.write(uploadPath, file.getBytes());

                // 公開URLを作成
                String imageUrl = FILE_URL_PREFIX + filename;
                imageUrls.add(imageUrl);

                // DBに保存
                ReviewImage reviewImage = ReviewImage.builder()
                        .imageUrl(imageUrl)
                        .contentType(contentType)
                        .createdAt(LocalDateTime.now().toString())
                        .build();
                reviewImageRepository.save(reviewImage);

            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ImageUploadResponseDto("画像保存に失敗しました", List.of()));
            }
        }

        return ResponseEntity.ok(new ImageUploadResponseDto("success", imageUrls));
    }

    /**
     * 指定された画像URLに対応するファイルとDBレコードを削除
     */
    @Override
    public ResponseEntity<ApiResponseDto> deleteImages(ImageDeleteRequestDto requestDto) {
        for (String url : requestDto.getImageUrls()) {
            try {
                // ファイル名（末尾）だけを抽出
                String filename = Paths.get(url).getFileName().toString();
                Path filePath = Paths.get(uploadDir).resolve(filename);

                // ファイル削除
                File file = filePath.toFile();
                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (!deleted) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(new ApiResponseDto("fail"));
                    }
                }

                // DB上のレコード削除
                reviewImageRepository.deleteByImageUrl(url);

            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponseDto("fail"));
            }
        }

        return ResponseEntity.ok(new ApiResponseDto("success"));
    }
}
