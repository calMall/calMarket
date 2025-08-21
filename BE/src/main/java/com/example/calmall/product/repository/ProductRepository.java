package com.example.calmall.product.repository;

import com.example.calmall.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


//商品エンティティに関するDB操作を行うリポジトリ
public interface ProductRepository extends JpaRepository<Product, String> {

    //  楽天の商品コード（itemCode）で商品を検索する
    Optional<Product> findByItemCode(String itemCode);
}
