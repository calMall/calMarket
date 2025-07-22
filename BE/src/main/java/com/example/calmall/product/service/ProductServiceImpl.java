package com.example.calmall.product.service;

import com.example.calmall.product.dto.ProductDetailResponseDto;
import com.example.calmall.product.entity.Product;
import com.example.calmall.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * 商品情報に関する処理を実装するサービスクラス
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    /**
     * 商品詳細取得処理
     */
    @Override
    public ResponseEntity<ProductDetailResponseDto> getProductDetail(String itemCode) {
        Product product = productRepository.findByItemCode(itemCode).orElseGet(() -> {
            // 商品がDBに存在しない場合は楽天APIから取得＆登録（ここでは簡略化してダミーデータ）
            Product newProduct = new Product();
            newProduct.setItemCode(itemCode);
            newProduct.setItemName("サンプル商品");
            newProduct.setItemCaption("この商品はサンプルです");
            newProduct.setCatchcopy("キャッチコピー例");
            newProduct.setPrice(1980);
            newProduct.setImages(List.of("https://example.com/sample.jpg"));
            newProduct.setInventory(new Random().nextInt(10)); // 0〜9のランダム在庫
            newProduct.setStatus(true);
            return productRepository.save(newProduct);
        });

        ProductDetailResponseDto.ProductDto dto = ProductDetailResponseDto.ProductDto.builder()
                .itemCode(product.getItemCode())
                .itemName(product.getItemName())
                .itemCaption(product.getItemCaption())
                .catchcopy(product.getCatchcopy())
                .score(4)  // 今回は仮値（レビュー処理があれば計算）
                .reviewCount(10) // 仮のレビュー件数
                .price(product.getPrice())
                .imageUrls(product.getImages())
                .build();

        ProductDetailResponseDto response = ProductDetailResponseDto.builder()
                .message("success")
                .product(dto)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 購入可能かどうかチェック（在庫 > 0）
     */
    @Override
    public ResponseEntity<Boolean> isPurchasable(String itemCode) {
        return productRepository.findByItemCode(itemCode)
                .map(product -> ResponseEntity.ok(product.getInventory() > 0))
                .orElse(new ResponseEntity<>(false, HttpStatus.NOT_FOUND));
    }
}