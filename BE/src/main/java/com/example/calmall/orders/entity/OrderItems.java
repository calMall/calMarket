package com.example.calmall.orders.entity;

import com.example.calmall.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

/**
 * 注文明細エンティティ（注文商品テーブル）
 * - 注文主（order）
 * - 商品コード（itemCode）
 * - 商品情報（product）
 * - 数量（quantity）
 * - 購入時価格（priceAtOrder）
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "order_items")
public class OrderItems {

    /** 明細ID（PK） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 注文主（FK → orders.id） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Orders order;

    /** 商品コード（FK → product.item_code） */
    @Column(name = "item_code")
    private String itemCode;

    /** 商品情報（Productエンティティへの参照） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_code", referencedColumnName = "item_code", insertable = false, updatable = false)
    private Product product;

    /** 数量 */
    private Integer quantity;

    /** 購入時価格 */
    @Column(name = "price_at_order")
    private Integer priceAtOrder;
}