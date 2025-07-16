package com.example.calmall.user.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
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
}
