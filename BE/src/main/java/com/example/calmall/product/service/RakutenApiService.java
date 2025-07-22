package com.example.calmall.product.service;

import com.example.calmall.product.entity.Product;

import java.util.Optional;

/**
 * 楽天APIとの通信を行うサービスインターフェース
 */
public interface RakutenApiService {

    /**
     * itemCode（商品コード）を使って楽天APIから商品情報を取得する
     * @param itemCode 楽天商品コード
     * @return 取得できた商品情報（なければ空）
     */
    Optional<Product> fetchProductFromRakuten(String itemCode);
}
