package com.example.calmall.product.service;

import com.example.calmall.product.dto.ProductDetailResponseDto;
import com.example.calmall.product.entity.Product;
import com.example.calmall.product.repository.ProductRepository;
import com.example.calmall.review.repository.ReviewRepository; // ★追加
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品情報に関する業務ロジックを実装するサービスクラス
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final RakutenApiService rakutenApiService;
    private final ReviewRepository reviewRepository; // ★レビュー情報の取得用

    /**
     * 商品詳細取得処理
     * @param itemCode 楽天商品コード
     * @return API仕様に沿ったレスポンス
     */
    @Override
    public ResponseEntity<ProductDetailResponseDto> getProductDetail(String itemCode) {

        // --- 1) DBから商品を取得（あれば） ---
        Product product = productRepository.findByItemCode(itemCode).orElse(null);
        if (product != null) {
            log.info("[ソース=DB] DBに既存商品を発見 itemCode={} name={}", product.getItemCode(), product.getItemName());
            return ResponseEntity.ok(buildSuccessResponse(product));
        }

        // --- 2) DBに無い → 楽天API照会 ---
        log.info("[ソース=DB無し→楽天API照会] itemCode={}", itemCode);
        product = rakutenApiService.fetchProductFromRakuten(itemCode).orElse(null);

        if (product == null) {
            log.warn("[ソース=楽天API無し] 楽天APIから商品取得不可 itemCode={}", itemCode);
            return new ResponseEntity<>(buildFailResponse(), HttpStatus.BAD_REQUEST);
        }

        // --- 3) API成功 → DB保存 ---
        Product saved = productRepository.save(product);
        log.info("[ソース=楽天API] 取得成功 → [DB登録] 完了 itemCode={} name={}", saved.getItemCode(), saved.getItemName());

        return ResponseEntity.ok(buildSuccessResponse(saved));
    }

    /**
     * 在庫による購入可否判定
     */
    @Override
    public ResponseEntity<Boolean> isPurchasable(String itemCode) {
        return productRepository.findByItemCode(itemCode)
                .map(product -> {
                    boolean purchasable = product.getInventory() != null && product.getInventory() > 0;
                    log.info("[ソース=DB] 購入可否判定 itemCode={} 在庫={} result={}", itemCode, product.getInventory(), purchasable);
                    return ResponseEntity.ok(purchasable);
                })
                .orElseGet(() -> {
                    log.warn("[ソース=DB無し] 購入可否判定対象商品がDBに存在しません itemCode={}", itemCode);
                    return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
                });
    }

    /**
     * 成功レスポンスDTOの組み立て
     */
    private ProductDetailResponseDto buildSuccessResponse(Product product) {
        // ★ 平均スコアとレビュー件数を取得
        Double score = reviewRepository.findAverageRatingByItemCode(product.getItemCode());
        int reviewCount = reviewRepository.countByProduct_ItemCode(product.getItemCode());

        ProductDetailResponseDto.ProductDto dto = ProductDetailResponseDto.ProductDto.builder()
                .itemCode(product.getItemCode())
                .itemName(product.getItemName())
                .itemCaption(product.getItemCaption())
                .catchcopy(product.getCatchcopy())
                .score(score != null ? Math.round(score * 10.0) / 10.0 : 0.0) // ★ 小数第1位で四捨五入
                .reviewCount(reviewCount)
                .price(product.getPrice())
                .imageUrls(product.getImages() != null ? product.getImages() : List.of())
                .build();

        return ProductDetailResponseDto.builder()
                .message("success")
                .product(dto)
                .build();
    }

    /**
     * 失敗レスポンスDTOの組み立て
     */
    private ProductDetailResponseDto buildFailResponse() {
        return ProductDetailResponseDto.builder()
                .message("fail")
                .product(null)
                .build();
    }
}
