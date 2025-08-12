package com.example.calmall.product.service;

import com.example.calmall.product.dto.ProductDetailResponseDto;
import com.example.calmall.product.entity.Product;
import com.example.calmall.product.repository.ProductRepository;
import com.example.calmall.product.text.DescriptionSuperCleaner; // ★ 超クリーン整形
import com.example.calmall.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品情報に関する業務ロジック（超クリーン版）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    // 商品エンティティを扱うリポジトリ
    private final ProductRepository productRepository;

    // 楽天APIから商品情報を取得するサービス
    private final RakutenApiService rakutenApiService;

    // レビュー関連情報の取得に使用
    private final ReviewRepository reviewRepository;

    /**
     * 商品詳細取得処理
     * 1) DBから取得。無ければ楽天APIから取得してDBへ保存
     * 2) 返却直前に説明文を「超・クリーン化」し、必要に応じてDBへ反映
     * 3) フロント互換のため、最終的に itemCaption はプレーン（超クリーン）で返す
     */
    @Override
    public ResponseEntity<ProductDetailResponseDto> getProductDetail(String itemCode) {

        // --- 1) DB から既存商品を検索 ---
        Product product = productRepository.findByItemCode(itemCode).orElse(null);

        // --- 2) 無ければ 楽天API から取得し保存 ---
        if (product == null) {
            log.info("[source=RakutenAPI] DB未登録 → 楽天API照会 itemCode={}", itemCode);
            product = rakutenApiService.fetchProductFromRakuten(itemCode).orElse(null);
            if (product == null) {
                log.warn("[not-found] 楽天APIから取得不可 itemCode={}", itemCode);
                return new ResponseEntity<>(buildFailResponse(), HttpStatus.BAD_REQUEST);
            }
            product = productRepository.save(product);
            log.info("[persist] 楽天APIから取得した商品を保存 itemCode={}", product.getItemCode());
        } else {
            log.info("[source=DB] 既存商品を取得 itemCode={} name={}", product.getItemCode(), product.getItemName());
        }

        // --- 3) 返却直前の「超・クリーン化」 ---
        //     DBにある descriptionHtml / descriptionPlain / itemCaption を入力に再構成
        String cleanHtml = DescriptionSuperCleaner.buildCleanHtml(
                product.getDescriptionHtml(),
                product.getDescriptionPlain(),
                product.getItemCaption()
        );
        String cleanPlain = DescriptionSuperCleaner.toPlain(cleanHtml); // フロント用プレーン

        boolean dirty = false;
        if (!equalsSafe(cleanHtml, product.getDescriptionHtml())) {
            product.setDescriptionHtml(cleanHtml);
            dirty = true;
        }
        if (!equalsSafe(cleanPlain, product.getDescriptionPlain())) {
            product.setDescriptionPlain(cleanPlain);
            dirty = true;
        }
        // フロント互換：itemCaption は常にプレーン（超クリーン）で保持
        if (!equalsSafe(cleanPlain, product.getItemCaption())) {
            product.setItemCaption(cleanPlain);
            dirty = true;
        }
        if (dirty) {
            product = productRepository.save(product); // ※DB反映が不要なら保存を外す
            log.info("[normalize] 説明文を超クリーン化して保存 itemCode={}", product.getItemCode());
        }

        // --- 4) 成功レスポンスを返却 ---
        return ResponseEntity.ok(buildSuccessResponse(product));
    }

    /**
     * 在庫による購入可否判定（inventory が 1 以上で購入可）
     */
    @Override
    public ResponseEntity<Boolean> isPurchasable(String itemCode) {
        return productRepository.findByItemCode(itemCode)
                .map(p -> ResponseEntity.ok(p.getInventory() != null && p.getInventory() > 0))
                .orElseGet(() -> new ResponseEntity<>(false, HttpStatus.BAD_REQUEST));
    }

    // -------------------- private helpers --------------------

    // null セーフな等価比較
    private boolean equalsSafe(String a, String b) {
        return (a == b) || (a != null && a.equals(b));
    }

    // 成功レスポンスDTOの組み立て（itemCaption は可読プレーン／HTMLも同梱）
    private ProductDetailResponseDto buildSuccessResponse(Product product) {
        Double score = reviewRepository.findAverageRatingByItemCode(product.getItemCode());
        int reviewCount = reviewRepository.countByProductItemCodeAndDeletedFalse(product.getItemCode());

        ProductDetailResponseDto.ProductDto dto = ProductDetailResponseDto.ProductDto.builder()
                .itemCode(product.getItemCode())
                .itemName(product.getItemName())
                .itemCaption(product.getItemCaption()) // フロントはこれを text として使用
                .catchcopy(product.getCatchcopy())
                .score(score != null ? Math.round(score * 10.0) / 10.0 : 0.0)
                .reviewCount(reviewCount)
                .price(product.getPrice())
                .imageUrls(product.getImages() != null ? product.getImages() : List.of())
                .descriptionPlain(product.getDescriptionPlain())
                .descriptionHtml(product.getDescriptionHtml())
                .build();

        return ProductDetailResponseDto.builder()
                .message("success")
                .product(dto)
                .build();
    }

    // 失敗レスポンスDTOの組み立て
    private ProductDetailResponseDto buildFailResponse() {
        return ProductDetailResponseDto.builder()
                .message("fail")
                .product(null)
                .build();
    }
}
