package com.example.calmall.user.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * ユーザーの配送先住所エンティティ
 */
@Entity
@Table(name = "user_delivery_addresses")
@Data
public class DeliveryAddress {

    /** 自動採番ID（主キー） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 郵便番号 */
    @Column(nullable = false)
    private String postalCode;

    /** 住所（都道府県など） */
    @Column(nullable = false)
    private String address1;

    /** 詳細住所（番地など） */
    @Column(nullable = false)
    private String address2;

    /** 対応するユーザー（外部キー） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
