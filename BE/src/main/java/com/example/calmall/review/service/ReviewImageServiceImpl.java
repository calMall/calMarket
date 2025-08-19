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
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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

    /**
     * 複数画像をアップロードする（JPG/PNGのみ・最大3枚）
     * － 新版：ローカル保存ではなく Cloudinary にアップロードし、DB には secure_url を保存
     */
    @Override
    public ResponseEntity<ImageUploadResponseDto> uploadImages(List<MultipartFile> files) {
        System.out.println("[DEBUG] uploadImages() が呼び出されました (Cloudinary)");

        // --- 枚数チェック（最大3枚） ---
        if (files == null || files.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ImageUploadResponseDto("画像が選択されていません", List.of()));
        }
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
                // --- Cloudinary にアップロード（フォルダ "reviews"） ---
                Map<?, ?> result = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "reviews",
                                "resource_type", "image"
                        )
                );

                String secureUrl = Objects.toString(result.get("secure_url"), null);
                if (secureUrl == null) {
                    System.out.println("[UPLOAD ERROR] Cloudinary returned null secure_url");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new ImageUploadResponseDto("画像保存に失敗しました", List.of()));
                }

                // --- レスポンス用のURLを追加 ---
                imageUrls.add(secureUrl);

                // --- DBに保存（レビュー未紐付け状態で登録） ---
                ReviewImage reviewImage = ReviewImage.builder()
                        .imageUrl(secureUrl)            // ← ここからは Cloudinary の URL を保存
                        .contentType(contentType)
                        .createdAt(LocalDateTime.now())
                        .build();
                reviewImageRepository.save(reviewImage);

                System.out.println("[UPLOAD] 画像保存成功 (Cloudinary): " + secureUrl);

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

    /**
     * 画像のURLリストを受け取り、対応するファイルとDBレコードを削除
     * － Cloudinary URL は Cloudinary 側も削除
     * － 旧式の /uploads/** はローカルから削除（後方互換）
     */
    @Override
    public ResponseEntity<ApiResponseDto> deleteImages(ImageDeleteRequestDto requestDto) {
        if (requestDto == null || requestDto.getImageUrls() == null || requestDto.getImageUrls().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDto("fail"));
        }

        for (String url : requestDto.getImageUrls()) {
            try {
                if (url == null || url.isBlank()) continue;

                // --- Cloudinary URL 削除 ---
                if (isCloudinaryUrl(url)) {
                    String publicId = guessPublicIdFromUrl(url);
                    if (publicId != null) {
                        Map<?, ?> res = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                        System.out.println("[DESTROY] Cloudinary publicId=" + publicId + " res=" + res);
                    } else {
                        System.out.println("[DESTROY] Cloudinary publicId 推定失敗: " + url);
                    }
                }

                // --- 旧式ローカルファイルの削除（/uploads/**） ---
                if (url.startsWith(FILE_URL_PREFIX)) {
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
                }

                // --- DBレコード削除 ---
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

    // ---- Helper ----

    private boolean isCloudinaryUrl(String url) {
        // 例: https://res.cloudinary.com/<cloud>/image/upload/v.../reviews/abc.png
        return url.startsWith("https://res.cloudinary.com/") && url.contains("/image/upload/");
    }

    /**
     * Cloudinary の secure_url から public_id を推定
     * 例:
     *   https://res.cloudinary.com/<cloud>/image/upload/v1724080530/reviews/abc.png
     *   → public_id = "reviews/abc"
     * ※ 現段階では DB に public_id カラムがない想定の暫定対応。
     *   将来的には review_images に public_id を追加して保存することを推奨。
     */
    private String guessPublicIdFromUrl(String secureUrl) {
        try {
            int idx = secureUrl.indexOf("/upload/");
            if (idx < 0) return null;
            String tail = secureUrl.substring(idx + "/upload/".length()); // v123.../reviews/abc.png
            // 先頭のバージョン vxxxx/ を除去
            if (tail.startsWith("v")) {
                int slash = tail.indexOf('/');
                if (slash > 0) tail = tail.substring(slash + 1);
            }
            int lastDot = tail.lastIndexOf('.');
            String noExt = (lastDot > 0) ? tail.substring(0, lastDot) : tail; // reviews/abc
            int q = noExt.indexOf('?');
            if (q >= 0) noExt = noExt.substring(0, q);
            return noExt;
        } catch (Exception e) {
            return null;
        }
    }
}
