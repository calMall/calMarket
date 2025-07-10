package com.example.calmall.repository;

import com.example.calmall.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

// 商品テーブルの操作を行うリポジトリ
public interface ProductRepository extends JpaRepository<Product, String> {}
