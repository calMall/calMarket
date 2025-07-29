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

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * レビュー画像のアップロード・削除機能を提供するサービスクラス
 */
@Service
@RequiredArgsConstructor
public class ReviewImageServiceImpl implements ReviewImageService {

    // application.properties に設定されたアップロードディレクトリ
    @Value("${file.upload-dir}")
    private String uploadDir;

    // 画像URLのprefix（例：/uploads/xxx.png）
    private static final String FILE_URL_PREFIX = "/uploads/";

    private final ReviewImageRepository reviewImageRepository;

    /**
     * サーバー起動後にアップロードパスを出力（デバッグ用）
     */
    @PostConstruct
    public void init() {
        System.out.println("[CONFIG] file.upload-dir: " + uploadDir);
    }

    /**
     * 画像アップロード処理（最大3枚・JPG/PNGのみ）
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

            if (!Objects.equals(contentType, "image/jpeg") && !Objects.equals(contentType, "image/png")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ImageUploadResponseDto("JPGまたはPNG形式のみアップロード可能です", List.of()));
            }

            try {
                String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String filename = UUID.randomUUID() + extension;

                Path uploadPath = Paths.get(uploadDir).resolve(filename);
                Files.write(uploadPath, file.getBytes());

                String imageUrl = FILE_URL_PREFIX + filename;
                imageUrls.add(imageUrl);

                ReviewImage reviewImage = ReviewImage.builder()
                        .imageUrl(imageUrl)
                        .contentType(contentType)
                        .createdAt(LocalDateTime.now())
                        .build();
                reviewImageRepository.save(reviewImage);

                System.out.println("[UPLOAD] 画像保存成功: " + imageUrl);

            } catch (IOException e) {
                System.out.println("[UPLOAD ERROR] ファイル保存失敗: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ImageUploadResponseDto("画像保存に失敗しました", List.of()));
            }
        }

        return ResponseEntity.ok(new ImageUploadResponseDto("success", imageUrls));
    }

    /**
     * アップロード済み画像の削除処理
     */
    @Override
    public ResponseEntity<ApiResponseDto> deleteImages(ImageDeleteRequestDto requestDto) {
        for (String url : requestDto.getImageUrls()) {
            try {
                // ファイル名抽出
                String filename = Paths.get(url).getFileName().toString();
                Path filePath = Paths.get(uploadDir).resolve(filename);

                System.out.println("[DELETE] 嘗試刪除路徑: " + filePath.toAbsolutePath());

                // ファイル削除（存在しなければスキップ）
                File file = filePath.toFile();
                if (file.exists()) {
                    if (file.delete()) {
                        System.out.println("[DELETE] ファイル削除成功: " + filename);
                    } else {
                        System.out.println("[DELETE WARNING] ファイル削除失敗（但繼續DB刪除）: " + filename);
                    }
                } else {
                    System.out.println("[DELETE] 対象ファイルが存在しません: " + filename);
                }

                // DB削除
                int count = reviewImageRepository.deleteByImageUrl(url);
                System.out.println("[DELETE] DB削除件数: " + count + "（URL: " + url + "）");

            } catch (Exception e) {
                System.out.println("[DELETE ERROR] 削除中に例外発生: " + e.getMessage());
                e.printStackTrace(); // ★ 例外の詳細をログ出力
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponseDto("fail"));
            }
        }

        return ResponseEntity.ok(new ApiResponseDto("success"));
    }
}
