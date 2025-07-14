package com.example.calmall.entity;

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

    private Integer userId; // ユーザーID

    private String productItemCode; // 商品ID（楽天形式）

    private String status; // 注文状態（例：CREATED, SHIPPED, DELIVERED）

    private LocalDateTime createdAt; // 注文作成日時
}
