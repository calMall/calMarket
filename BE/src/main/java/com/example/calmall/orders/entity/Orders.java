package com.example.calmall.orders.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

<<<<<<< HEAD
// 注文情報を管理するエンティティ
=======
/**
 * 注文情報を管理するエンティティ
 */
>>>>>>> BE
@Entity
@Data
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
<<<<<<< HEAD
    private Long id; // 注文ID

    private Integer userId; // 注文を行ったユーザーID

    private String itemCode; // 商品ID（楽天形式）

    private String status; // 注文状態（CREATED, SHIPPED, DELIVEREDなど）

    private LocalDateTime createdAt; // 作成日時
=======
    private Long id;

    // ユーザーの主キーID（User.id を参照）
    private Long userId;

    // 商品ID（楽天形式）
    private String productItemCode;

    // 注文状態（例：CREATED, SHIPPED, DELIVERED, REFUNDED）
    private String status;

    // 注文作成日時
    private LocalDateTime createdAt;
>>>>>>> BE
}
