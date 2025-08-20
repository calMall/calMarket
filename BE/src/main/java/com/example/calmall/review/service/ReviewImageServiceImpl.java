package com.example.calmall.review.service;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.review.dto.ImageUploadResponseDto;
import com.example.calmall.review.dto.ImageDeleteRequestDto;
import com.example.calmall.review.entity.ReviewImage;
import com.example.calmall.review.repository.ReviewImageRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;


/**
 * レビュー画像のアップロード・削除機能を提供するサービスクラス
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewImageServiceImpl implements ReviewImageService {

    // 旧ローカル保存用ディレクトリ（後方互換の削除処理でのみ使用）
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private static final String FILE_URL_PREFIX = "/uploads/";

    private final ReviewImageRepository reviewImageRepository;

    // Cloudinary（CloudinaryConfig で Bean 化）
    private final Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        System.out.println("[CONFIG] file.upload-dir: " + uploadDir);
        System.out.println("[CONFIG] Cloudinary ready");
    }


    // 複数画像をアップロードする（JPG/PNGのみ最大3枚）
    @Override
    public ResponseEntity<ImageUploadResponseDto> uploadImages(List<MultipartFile> files) {
        System.out.println("[DEBUG] uploadImages() が呼び出されました (Cloudinary)");

        // 枚数チェック（最大3枚）
        if (files == null || files.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ImageUploadResponseDto("画像が選択されていません", List.of()));
        }
        if (files.size() > 3) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ImageUploadResponseDto("画像は最大3枚までです", List.of()));
        }

        // --- 同一リクエスト内での重複ファイルを除外する処理 ---
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
                // --- Cloudinary にアップロード（フォルダ "reviews"） ---
                Map<?, ?> result = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "reviews",
                                "resource_type", "image"
                        )
                );

                String secureUrl = Objects.toString(result.get("secure_url"), null);
                String publicId = Objects.toString(result.get("public_id"), null); // ★ public_id 取得

                if (secureUrl == null) {
                    System.out.println("[UPLOAD ERROR] Cloudinary returned null secure_url");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new ImageUploadResponseDto("画像保存に失敗しました", List.of()));
                }

                // --- レスポンス用のURLを追加 ---
                imageUrls.add(secureUrl);

                // --- DBに保存（レビュー未紐付け状態で登録） ---
                ReviewImage reviewImage = ReviewImage.builder()
                        .imageUrl(secureUrl)     // Cloudinary の URL を保存
                        .publicId(publicId)      // ★ public_id を保存
                        .contentType(contentType)
                        .createdAt(LocalDateTime.now())
                        .build();
                reviewImageRepository.save(reviewImage);

                System.out.println("[UPLOAD] 画像保存成功 (Cloudinary): " + secureUrl + " publicId=" + publicId);

            } catch (IOException e) {
                System.out.println("[UPLOAD ERROR] Cloudinary 送信失敗: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ImageUploadResponseDto("画像保存に失敗しました", List.of()));
            } catch (Exception e) {
                System.out.println("[UPLOAD ERROR] 予期せぬ例外: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ImageUploadResponseDto("画像保存に失敗しました", List.of()));
            }
        }

        // --- 成功したURLのみ返却 ---
        return ResponseEntity.ok(new ImageUploadResponseDto("success", imageUrls));
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponseDto> deleteImages(ImageDeleteRequestDto requestDto) {
        List<String> failedUrls = new ArrayList<>();

        for (String url : requestDto.getImageUrls()) {
            try {
                Optional<ReviewImage> opt = reviewImageRepository.findByImageUrl(url);
                if (opt.isPresent()) {
                    ReviewImage img = opt.get();

                    // --- Cloudinary 側削除 ---
                    try {
                        cloudinary.uploader().destroy(img.getPublicId(),
                                ObjectUtils.asMap("resource_type", "image", "invalidate", true));
                    } catch (Exception e) {
                        log.error("Cloudinary delete failed: url={} publicId={}", url, img.getPublicId(), e);
                        failedUrls.add(url);
                        continue;
                    }

                    // --- DB 側削除 ---
                    int deleted = reviewImageRepository.deleteByImageUrl(url);
                    if (deleted == 0) {
                        failedUrls.add(url);
                    }
                } else {
                    // DB に存在しない場合 → 失敗リストへ
                    failedUrls.add(url);
                }

            } catch (Exception e) {
                log.error("DeleteImages error url={}", url, e);
                failedUrls.add(url);
            }
        }

        if (!failedUrls.isEmpty()) {
            // 部分または全部失敗 → 404
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDto("fail: 削除できなかったURL -> " + failedUrls));
        }

        return ResponseEntity.ok(new ApiResponseDto("success"));
    }




    // Helper
    private boolean isCloudinaryUrl(String url) {
        return url != null
                && url.startsWith("https://res.cloudinary.com/")
                && url.contains("/image/upload/");
    }
}
