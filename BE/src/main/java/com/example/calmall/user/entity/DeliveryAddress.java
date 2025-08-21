package com.example.calmall.user.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * ユーザーの配送先住所を管理するエンティティクラス
 */
@Entity
@Table(name = "user_delivery_addresses")
@Data
public class DeliveryAddress {

    /** 主キー（自動採番） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 郵便番号（例：123-4567） */
    @Column(nullable = false)
    private String postalCode;

    /** 住所1（都道府県、市区町村など） */
    @Column(nullable = false)
    private String address1;

    /** 住所2（建物名、部屋番号など） */
    @Column(nullable = false)
    private String address2;

    /**
     * 紐づくユーザー情報（外部キー）
     * - ここで user_id を UUID(user.userId) にリンクさせる
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    private User user;
}
