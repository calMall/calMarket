package com.example.calmall.orders.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 注文情報を管理するエンティティ
 */
@Entity
@Data
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ユーザーの主キーID（User.id を参照）
    private Long userId;

    // 商品ID（楽天形式）
    private String productItemCode;

    // 注文状態（例：CREATED, SHIPPED, DELIVERED, REFUNDED）
    private String status;

    // 注文作成日時
    private LocalDateTime createdAt;
}
