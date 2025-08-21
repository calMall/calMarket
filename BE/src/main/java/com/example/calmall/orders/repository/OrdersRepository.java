package com.example.calmall.orders.repository;

import com.example.calmall.orders.entity.Orders;
import com.example.calmall.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 注文履歴を扱うJPAリポジトリ
 */
public interface OrdersRepository extends JpaRepository<Orders, Long> {

    /**
     * 指定ユーザーが指定商品の購入履歴を
     * 指定日時以降に持っているかチェック
     */
    @Query("""
        SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END
        FROM Orders o
        JOIN o.orderItems oi
        WHERE o.user.userId = :userId
          AND oi.product.itemCode = :itemCode  
          AND o.createdAt > :after
    """)
    boolean existsPurchaseWithinPeriod(
            @Param("userId") String userId,
            @Param("itemCode") String itemCode,
            @Param("after") LocalDateTime after
    );

    /**
     * 指定ユーザーが指定商品を購入した全注文を取得
     */
    @Query("""
        SELECT o
        FROM Orders o
        JOIN o.orderItems oi
        WHERE o.user.userId = :userId
          AND oi.product.itemCode = :itemCode 
    """)
    List<Orders> findOrdersByUserAndItemCode(
            @Param("userId") String userId,
            @Param("itemCode") String itemCode
    );

    List<Orders> findTop10ByUserOrderByCreatedAtDesc(User user);

    //ユーザの注文履歴
    List<Orders> findByUser_UserId(String userId);
    //ページネーション
    Page<Orders> findByUser_UserId(String userId, Pageable pageable);
    //注文詳細
    Optional<Orders> findByIdAndUser_UserId(Long orderId, String userId);
    
}