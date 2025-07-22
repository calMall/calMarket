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

    // 所属ユーザーID（User.id を参照）
    private Long userId;

    // 商品ID（楽天形式）
    private String itemCode;

    // 数量（0になったら削除）
    private Integer quantity;

    // APIから与えられる商品オプション
    private String option;
}
