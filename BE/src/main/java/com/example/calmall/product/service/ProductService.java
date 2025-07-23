package com.example.calmall.product.service;

import com.example.calmall.product.dto.ProductDetailResponseDto;
import org.springframework.http.ResponseEntity;

/**
 * 商品情報サービスインターフェース。
 */
public interface ProductService {

    /**
     * 商品詳細取得API（GET /api/products/{itemCode}）。
     */
    ResponseEntity<ProductDetailResponseDto> getProductDetail(String itemCode);

    /**
     * 購入可否チェックAPI（GET /api/products/{itemCode}/purchasable）。
     * true=購入可 / false=購入不可。
     */
    ResponseEntity<Boolean> isPurchasable(String itemCode);
}
