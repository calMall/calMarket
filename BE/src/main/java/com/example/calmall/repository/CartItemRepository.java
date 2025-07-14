package com.example.calmall.repository;

import com.example.calmall.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

// カートアイテムテーブルの操作を行うリポジトリ
public interface CartItemRepository extends JpaRepository<CartItem, Long> {}
