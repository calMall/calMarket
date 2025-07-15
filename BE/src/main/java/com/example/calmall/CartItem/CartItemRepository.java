package com.example.calmall.CartItem;

import org.springframework.data.jpa.repository.JpaRepository;

// カートアイテムテーブルの操作を行うリポジトリ
public interface CartItemRepository extends JpaRepository<CartItem, Long> {}
