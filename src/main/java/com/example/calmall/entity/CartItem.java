package com.example.calmall.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * カート内商品を管理する中間エンティティ
 */
@Entity
@Data
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer userId; // カート所有者のユーザーID

    private String productId; // 商品ID（楽天形式）

    private Integer quantity; // 数量

    private String option; // オプション情報（API側で指定された内容）
}
