package com.example.calmall.CartItem;

import jakarta.persistence.*;
import lombok.Data;

// ユーザーのカート情報を管理する中間エンティティ
@Entity
@Data
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // カートアイテムID

    private Integer userId; // 所属ユーザーID

    private String itemCode; // 商品ID（楽天形式）

    private Integer quantity; // 数量

    private String option; // APIから与えられる商品オプション
}
