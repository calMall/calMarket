package com.example.calmall.review.service;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.review.dto.ImageUploadResponseDto;
import com.example.calmall.review.dto.ImageDeleteRequestDto;
import com.example.calmall.review.entity.ReviewImage;
import com.example.calmall.review.repository.ReviewImageRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
 * レビュー画像のアップロード・削除機能を提供するサービスクラス
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewImageServiceImpl implements ReviewImageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private static final String FILE_URL_PREFIX = "/uploads/";

    private final ReviewImageRepository reviewImageRepository;

    @PostConstruct
    public void init() {
        System.out.println("[CONFIG] file.upload-dir: " + uploadDir);
    }

    /**
     * 複数画像をアップロードする（JPG/PNGのみ・最大3枚）
     */
    @Override
    public ResponseEntity<ImageUploadResponseDto> uploadImages(List<MultipartFile> files) {
        System.out.println("[DEBUG] uploadImages() が呼び出されました");

        // --- 枚数チェック（最大3枚） ---
        if (files.size() > 3) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ImageUploadResponseDto("画像は最大3枚までです", List.of()));
        }

        // --- 同一リクエスト内での重複ファイルを除外する処理 ---
        // key として「元ファイル名 + サイズ」を利用して一意性を判定
        Set<String> seenFileKeys = new HashSet<>();
        List<MultipartFile> uniqueFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            String key = file.getOriginalFilename() + "-" + file.getSize();
            if (seenFileKeys.add(key)) {
                // 初めてのファイル → 処理対象に追加
                uniqueFiles.add(file);
            } else {
                // 同一ファイルが既に存在 → スキップ
                System.out.println("[SKIP] 同一リクエスト内で重複したファイル: " + file.getOriginalFilename());
            }
        }

        // 実際にアップロードされた画像のURLを格納するリスト
        List<String> imageUrls = new ArrayList<>();

        // --- 各ファイルを順に処理 ---
        for (MultipartFile file : uniqueFiles) {
            // --- ファイル形式チェック ---
            String contentType = file.getContentType();
            if (!Objects.equals(contentType, "image/jpeg") && !Objects.equals(contentType, "image/png")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ImageUploadResponseDto("JPGまたはPNG形式のみアップロード可能です", List.of()));
            }

            try {
                // --- 新しいファイル名（UUID）を生成 ---
                String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String filename = UUID.randomUUID() + extension;
                String imageUrl = FILE_URL_PREFIX + filename;

                // --- DB上に同一URLが存在するかチェック（理論上は衝突しないが保険として） ---
                if (reviewImageRepository.findByImageUrl(imageUrl).isPresent()) {
                    System.out.println("[SKIP] DB上に既に存在するURL: " + imageUrl);
                    continue;
                }

                // --- ファイル保存 ---
                Path uploadPath = Paths.get(uploadDir).resolve(filename);
                Files.write(uploadPath, file.getBytes());

                // --- レスポンス用のURLを追加 ---
                imageUrls.add(imageUrl);

                // --- DBに保存（レビュー未紐付け状態で登録） ---
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

        // --- 成功したURLのみ返却 ---
        return ResponseEntity.ok(new ImageUploadResponseDto("success", imageUrls));
    }

    /**
     * 画像のURLリストを受け取り、対応するファイルとDBレコードを削除
     */
    @Override
    public ResponseEntity<ApiResponseDto> deleteImages(ImageDeleteRequestDto requestDto) {
        for (String url : requestDto.getImageUrls()) {
            try {
                String filename = Paths.get(url).getFileName().toString();
                Path filePath = Paths.get(uploadDir).resolve(filename);
                System.out.println("[DELETE] 嘗試刪除路徑: " + filePath.toAbsolutePath());

                File file = filePath.toFile();
                if (file.exists()) {
                    if (file.delete()) {
                        System.out.println("[DELETE] ファイル削除成功: " + filename);
                    } else {
                        System.out.println("[DELETE WARNING] ファイル削除失敗（但DB削除は継続）: " + filename);
                    }
                } else {
                    System.out.println("[DELETE] 対象ファイルが存在しません: " + filename);
                }

                int count = reviewImageRepository.deleteByImageUrl(url);
                System.out.println("[DELETE] DB削除件数: " + count + "（URL: " + url + "）");

            } catch (Exception e) {
                System.out.println("[DELETE ERROR] 削除中に例外発生: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponseDto("fail"));
            }
        }

        return ResponseEntity.ok(new ApiResponseDto("success"));
    }
}
