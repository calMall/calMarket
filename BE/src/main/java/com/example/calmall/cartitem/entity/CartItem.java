package com.example.calmall.cartitem.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * ユーザーのカート情報を管理する中間エンティティ
 */
@Entity
@Data
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 所属ユーザーID（User.userId を参照）
    @Column(name = "user_id", nullable = false)
    private String userId;

    // 商品ID（楽天形式）
    @Column(name = "item_code", nullable = false)
    private String itemCode;

    // 数量（0になったら削除）
    private Integer quantity;

    // APIから与えられる商品オプション
    private String option;
}
