package com.example.calmall.product.service;

import com.example.calmall.product.entity.Product;

import java.util.Optional;


// 楽天APIとの通信を行うサービスインターフェース
public interface RakutenApiService {


    //　itemCode商品コード
    Optional<Product> fetchProductFromRakuten(String itemCode);
}
