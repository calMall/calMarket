package com.example.calmall.repository;

import com.example.calmall.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 商品エンティティに関するDB操作を行うリポジトリ
 */
public interface ProductRepository extends JpaRepository<Product, String> {

    /**
     * 楽天の商品コード（itemCode）で商品を検索する
     * @param itemCode 楽天商品コード
     * @return 商品が存在すればOptionalにラップして返す
     */
    Optional<Product> findByItemCode(String itemCode);
}
