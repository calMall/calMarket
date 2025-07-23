package com.example.calmall.product.service;

import com.example.calmall.product.dto.ProductDetailResponseDto;
import com.example.calmall.product.entity.Product;
import com.example.calmall.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * 商品情報に関するビジネスロジック実装クラス。
 *
 * フロント側想定フロー：
 * ・ユーザーが商品をクリック → GET /api/products/{itemCode} を呼び出す
 *   → DB未登録なら楽天APIから取得して保存（このタイミングでDB登録）
 * ・カート／注文画面で在庫確認時 → GET /api/products/{itemCode}/purchasable
 *   → DBに無い商品は未登録扱い（自動登録しない）→ 400 + false
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final RakutenApiService rakutenApiService;

    /**
     * 商品詳細取得処理。
     * DBに無い場合は楽天APIから取得して保存。
     * 成功時：message=success, HTTP 200
     * 失敗時（楽天でも取得不可）：message=fail, HTTP 400
     */
    @Override
    @Transactional
    public ResponseEntity<ProductDetailResponseDto> getProductDetail(String itemCode) {

        // DB検索 → 無ければ楽天APIから取得・保存
        Product product = productRepository.findByItemCode(itemCode).orElseGet(() ->
                rakutenApiService.fetchProductFromRakuten(itemCode)
                        .map(productRepository::save)
                        .orElse(null)
        );

        // 楽天でも取得不可 → fail & 400
        if (product == null) {
            ProductDetailResponseDto fail = ProductDetailResponseDto.builder()
                    .message("fail")
                    .product(null)
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(fail);
        }

        // エンティティ→DTO変換
        ProductDetailResponseDto.ProductDto dto = ProductDetailResponseDto.ProductDto.builder()
                .itemCode(product.getItemCode())
                .itemName(product.getItemName())
                .itemCaption(product.getItemCaption())
                .catchcopy(product.getCatchcopy())
                // TODO: レビュー機能実装後に置換
                .score(4)
                .reviewCount(10)
                .price(product.getPrice())
                .imageUrls(product.getImages() != null ? product.getImages() : Collections.emptyList())
                .build();

        ProductDetailResponseDto success = ProductDetailResponseDto.builder()
                .message("success")
                .product(dto)
                .build();

        return ResponseEntity.ok(success);
    }

    /**
     * 購入可否チェック処理（自動登録なし）。
     * DBに未登録の商品は 400 + false を返却。
     * DBに存在する場合のみ在庫で判定（inventory > 0）。
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Boolean> isPurchasable(String itemCode) {

        return productRepository.findByItemCode(itemCode)
                .map(p -> ResponseEntity.ok(p.getInventory() != null && p.getInventory() > 0))
                // 商品未登録
                .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(false));
    }
}
