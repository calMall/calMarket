package com.example.calmall.product.service;

import com.example.calmall.product.entity.Product;

import java.util.Optional;

/**
 * 楽天APIとの通信を行うサービスインターフェース
 *
 * itemCode をもとに楽天の Ichiba API を呼び出し、
 * 商品情報を取得して Product エンティティ相当のオブジェクトに詰め替えて返却する。
 * 商品が見つからない／APIエラーの場合は Optional.empty() を返す。</p>
 */
public interface RakutenApiService {

    /**
     * itemCode（商品コード）を使って楽天APIから商品情報を取得する。
     *
     * @param itemCode 楽天商品コード（例：jpntnc:10002379）※「:」などが含まれる場合がある
     * @return 取得できた商品情報（なければ空）
     */
    Optional<Product> fetchProductFromRakuten(String itemCode);
}
