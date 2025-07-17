package com.example.calmall.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 会員（ユーザー）情報を管理するエンティティ
 */
@Entity
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 15)
    private String userId; // ユーザー識別ID（文字列）

    @Column(length = 10)
    private String nickname; // ニックネーム

    @Column(length = 128)
    private String email; // メールアドレス

    private LocalDate birth; // 生年月日

    @ElementCollection
    private List<String> deliveryAddresses; // 配送先住所のリスト

    private Integer point; // 保有ポイント
}
