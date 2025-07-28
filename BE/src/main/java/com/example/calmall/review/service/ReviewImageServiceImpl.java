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
 * レビュー画像をアップロード・削除するサービスクラスの実装
 */
@Service
@RequiredArgsConstructor
public class ReviewImageServiceImpl implements ReviewImageService {

    // application.properties に定義されたアップロードディレクトリのパス
    @Value("${file.upload-dir}")
    private String uploadDir;

    // 画像公開用URLのパスプレフィックス
    private static final String FILE_URL_PREFIX = "/uploads/";

    // ReviewImage テーブルへのアクセス用リポジトリ
    private final ReviewImageRepository reviewImageRepository;

    /**
     * 複数画像をアップロードし、サーバー上に保存・DB登録する処理
     * JPG/PNGのみ対応、最大3枚まで
     */
    @Override
    public ResponseEntity<ImageUploadResponseDto> uploadImages(List<MultipartFile> files) {
        // 画像数が3枚を超える場合はエラーを返す
        if (files.size() > 3) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ImageUploadResponseDto("画像は最大3枚までです", List.of()));
        }

        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            String contentType = file.getContentType();

            // JPGまたはPNG以外はエラー
            if (!Objects.equals(contentType, "image/jpeg") && !Objects.equals(contentType, "image/png")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ImageUploadResponseDto("JPGまたはPNG形式のみアップロード可能です", List.of()));
            }

            try {
                // オリジナルファイル名と拡張子を取得
                String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

                // UUID付きファイル名を生成
                String filename = UUID.randomUUID() + extension;

                // 保存パスを作成してファイルを保存
                Path uploadPath = Paths.get(uploadDir).resolve(filename);
                Files.write(uploadPath, file.getBytes());

                // 公開用の画像URLを作成
                String imageUrl = FILE_URL_PREFIX + filename;
                imageUrls.add(imageUrl);

                // DBに保存
                ReviewImage reviewImage = ReviewImage.builder()
                        .imageUrl(imageUrl)
                        .contentType(contentType)
                        .createdAt(LocalDateTime.now().toString())
                        .build();
                reviewImageRepository.save(reviewImage);

                System.out.println("[UPLOAD] 画像保存成功: " + imageUrl);

            } catch (IOException e) {
                System.out.println("[UPLOAD ERROR] ファイル書き込み失敗: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ImageUploadResponseDto("画像保存に失敗しました", List.of()));
            }
        }

        return ResponseEntity.ok(new ImageUploadResponseDto("success", imageUrls));
    }

    /**
     * 画像URLリストを受け取り、該当ファイルとDB上のレコードを削除
     */
    @Override
    public ResponseEntity<ApiResponseDto> deleteImages(ImageDeleteRequestDto requestDto) {
        for (String url : requestDto.getImageUrls()) {
            try {
                // ファイル名をパスから抽出（例: /uploads/abc.png → abc.png）
                String filename = Paths.get(url).getFileName().toString();
                Path filePath = Paths.get(uploadDir).resolve(filename);

                // ファイルを削除
                File file = filePath.toFile();
                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        System.out.println("[DELETE] ファイル削除成功: " + filename);
                    } else {
                        System.out.println("[DELETE ERROR] ファイル削除失敗: " + filename);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(new ApiResponseDto("fail"));
                    }
                } else {
                    System.out.println("[DELETE] 対象ファイルなし: " + filename);
                }

                // DBから該当URLのレコードを削除
                int count = reviewImageRepository.deleteByImageUrl(url);
                System.out.println("[DELETE] DB削除件数: " + count + "（対象URL: " + url + "）");

            } catch (Exception e) {
                System.out.println("[DELETE ERROR] 処理中に例外発生: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponseDto("fail"));
            }
        }

        return ResponseEntity.ok(new ApiResponseDto("success"));
    }
}
