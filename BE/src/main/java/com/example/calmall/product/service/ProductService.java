package com.example.calmall.product.service;

import com.example.calmall.product.dto.ProductDetailResponseDto;
import org.springframework.http.ResponseEntity;

/**
 * 商品情報に関するビジネスロジックを提供するサービスインターフェース
 */
public interface ProductService {

    ResponseEntity<ProductDetailResponseDto> getProductDetail(String itemCode);

    ResponseEntity<Boolean> isPurchasable(String itemCode);
}
