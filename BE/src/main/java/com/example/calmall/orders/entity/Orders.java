package com.example.calmall.orders.entity;

import com.example.calmall.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 注文エンティティ（注文主テーブル）
 * - ユーザー情報（user）
 * - 注文状態（status）
 * - 作成日時（createdAt）
 * - 注文明細（orderItems）
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orders")
public class Orders {

    /** 注文ID（PK） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 注文ユーザー（FK → users.user_id） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    private User user;

    /** 注文状態（例: PENDING, SHIPPED, DELIVERED, REFUNDED） */
    private String status;

    /** 作成日時 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 注文明細（1つの注文に複数の商品） */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItems> orderItems = new ArrayList<>();
}
