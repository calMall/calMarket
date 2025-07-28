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
 * アップロード画像の保存・削除・DB登録などを処理するサービス実装クラス
 */
@Service
@RequiredArgsConstructor
public class ReviewImageServiceImpl implements ReviewImageService {

    // application.properties で設定されたアップロード先ディレクトリ
    @Value("${file.upload-dir}")
    private String uploadDir;

    // クライアントに返却する相対パスの接頭辞
    private static final String FILE_URL_PREFIX = "/uploads/";

    // データベースアクセス用のリポジトリ
    private final ReviewImageRepository reviewImageRepository;

    /**
     * 複数画像（最大3枚）のアップロード処理
     */
    @Override
    public ResponseEntity<ImageUploadResponseDto> uploadImages(List<MultipartFile> files) {
        if (files.size() > 3) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ImageUploadResponseDto("画像は最大3枚までです", List.of()));
        }

        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            String contentType = file.getContentType();

            // JPGまたはPNG形式のみ許可
            if (!Objects.equals(contentType, "image/jpeg") && !Objects.equals(contentType, "image/png")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ImageUploadResponseDto("JPGまたはPNG形式のみアップロード可能です", List.of()));
            }

            try {
                // オリジナルのファイル名と拡張子を取得
                String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

                // UUIDを付与したファイル名を生成
                String filename = UUID.randomUUID() + extension;

                // 保存先のパスを生成
                Path uploadPath = Paths.get(uploadDir).resolve(filename);
                Files.write(uploadPath, file.getBytes());

                // 相対パスの画像URLを構築
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
     * 指定された画像URLリストに基づき、ファイルとDBレコードを削除する
     */
    @Override
    public ResponseEntity<ApiResponseDto> deleteImages(ImageDeleteRequestDto requestDto) {
        for (String url : requestDto.getImageUrls()) {
            try {
                // ファイル名（末尾部分）を抽出
                String filename = Paths.get(url).getFileName().toString();
                Path filePath = Paths.get(uploadDir).resolve(filename);

                // 該当ファイルを削除
                File file = filePath.toFile();
                if (file.exists() && !file.delete()) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new ApiResponseDto("fail"));
                }

                // DBから該当画像レコードを削除
                reviewImageRepository.deleteByImageUrl(url);

            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponseDto("fail"));
            }
        }

        return ResponseEntity.ok(new ApiResponseDto("success"));
    }
}
