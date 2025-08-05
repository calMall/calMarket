package com.example.calmall.orders.entity;

import com.example.calmall.product.entity.Product;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "order_items")
public class OrderItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Orders order;

    // 商品へのリレーションシップのみを定義し、itemCodeはエンティティから取得する
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_code", referencedColumnName = "item_code", nullable = false)
    private Product product;

    private Integer quantity;

    @Column(name = "price_at_order")
    private Double priceAtOrder; // データ型をDoubleに変更

    @Column(name = "image_list_urls", length = 1000)
    private String imageListUrls;

    @Column(name = "item_name", nullable = false)
    private String itemName;
    
    // このフィールドは削除
    // @Column(name = "item_code")
    // private String itemCode;

}