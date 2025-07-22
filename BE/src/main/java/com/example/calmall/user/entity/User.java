package com.example.calmall.user.entity;

<<<<<<< HEAD
=======
import com.example.calmall.user.entity.DeliveryAddress;
>>>>>>> BE
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
<<<<<<< HEAD
 * ユーザー情報を管理するエンティティ
 */
@Entity
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ユーザー内部ID

    @Column(length = 15)
    private String userId; // 表示用ユーザーID

    @Column(length = 10)
    private String nickname; // ニックネーム

    @Column(length = 128)
    private String email; // メールアドレス

    @Column(length = 128, nullable = false)
    private String password; // パスワード

    private LocalDate birth; // 生年月日

    @ElementCollection
    private List<String> deliveryAddresses; // 配送先住所のリスト

    private Integer point; // 所持ポイント
=======
 * 会員（ユーザー）情報を管理するエンティティ
 * - Long型の内部ID（id）を主キーとして使用
 * - 外部連携などに使用するUUID形式のuserIdも保持
 */
@Entity
@Table(name = "\"user\"") // PostgreSQL の予約語回避のためエスケープ
@Data
public class User {

    /** 内部管理用ID（自動採番、主キー） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 外部公開用のUUID（ユニークかつ非NULL） */
    @Column(length = 40, unique = true, nullable = false)
    private String userId;

    /** ニックネーム（最大10文字） */
    @Column(length = 10)
    private String nickname;

    /** メールアドレス（最大128文字、ユニーク） */
    @Column(length = 128, unique = true)
    private String email;

    /** パスワード（最大64文字） */
    @Column(length = 64)
    private String password;

    /** 生年月日（任意） */
    private LocalDate birth;

    /** 配送先住所（1対多リレーション） */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DeliveryAddress> deliveryAddresses;

    /** 保有ポイント（初期値0） */
    private Integer point = 0;
>>>>>>> BE
}
