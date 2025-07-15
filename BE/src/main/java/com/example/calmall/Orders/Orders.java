package com.example.calmall.Orders;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

// 注文情報を管理するエンティティ
@Entity
@Data
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 注文ID

    private Integer userId; // 注文を行ったユーザーID

    private String itemCode; // 商品ID（楽天形式）

    private String status; // 注文状態（CREATED, SHIPPED, DELIVEREDなど）

    private LocalDateTime createdAt; // 作成日時
}
