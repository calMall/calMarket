// ✅ CartItemRepository.java
package com.example.calmall.cartitem.repository;

import com.example.calmall.cartitem.entity.CartItem;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

// カートアイテムテーブルの操作を行うリポジトリ
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserIdAndIdIn(String userId, List<Long> ids);
    Optional<CartItem> findByUserIdAndId(String userId, Long id);
}
