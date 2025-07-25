package com.example.calmall.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 会員（ユーザー）情報を管理するエンティティ
 * - Long型の内部ID（id）を主キーとして使用
 * - 外部連携などに使用するUUID形式のuserIdも保持
 */
@Entity
@Table(name = "users")
@Data
public class User {

    /** 内部管理用ID（自動採番、主キー） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 外部公開用のUUID（ユニークかつ非NULL） */
    @Column(name = "user_id", length = 40, unique = true, nullable = false)
    private String userId;

    /** ニックネーム（最大10文字） */
    @Column(length = 10)
    private String nickname;

    /** メールアドレス（最大128文字、ユニーク） */
    @Column(length = 128, unique = true)
    private String email;

    /** パスワード（8〜64文字） */
    @Column(length = 64)
    private String password;

    /** 生年月日（任意） */
    private LocalDate birth;

    /** 配送先住所（1対多リレーション） */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DeliveryAddress> deliveryAddresses;

    /** 保有ポイント（初期値0） */
    private Integer point = 0;
}
