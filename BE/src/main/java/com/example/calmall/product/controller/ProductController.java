package com.example.calmall.product.controller;

import com.example.calmall.product.dto.ProductDetailResponseDto;
import com.example.calmall.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


// 商品関連のAPIを提供するコントローラークラス
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;


    // 商品詳細を取得するAPI
    @GetMapping("/{itemCode}")
    public ResponseEntity<ProductDetailResponseDto> getProductDetail(@PathVariable String itemCode) {
        return productService.getProductDetail(itemCode);
    }

    // 購入可能かどうかをチェックするAPI
    @GetMapping("/{itemCode}/purchasable")
    public ResponseEntity<Boolean> checkPurchasable(@PathVariable String itemCode) {
        return productService.isPurchasable(itemCode);
    }
}
