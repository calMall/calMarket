// ✅ CartItemRepository.java
package com.example.calmall.cartitem.repository;

import com.example.calmall.cartitem.entity.CartItem;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// カートアイテムテーブルの操作を行うリポジトリ
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserIdAndIdIn(String userId, List<Long> ids);
    Optional<CartItem> findByUserIdAndId(String userId, Long id);
    List<CartItem> findByUserId(String userId);
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.itemCode IN :itemCodes AND ci.userId = :userId")
    void deleteByItemCodeInAndUserId(@Param("itemCodes") List<String> itemCodes, @Param("userId") String userId);
}
