package com.example.calmall.orders.entity;

import com.example.calmall.user.entity.User;
<<<<<<< HEAD
import com.fasterxml.jackson.annotation.JsonIgnore;

=======
>>>>>>> e5416f9e4f813fe058e65f4eff2a5fc9c03e5b53
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

<<<<<<< HEAD
=======
    /** 注文ユーザー（FK → users.user_id） */
>>>>>>> e5416f9e4f813fe058e65f4eff2a5fc9c03e5b53
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    @JsonIgnore
    private User user;

<<<<<<< HEAD
    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "status", nullable = false)
    private String status; // String型に戻す

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}
=======
    /** 注文状態（例: PENDING, SHIPPED, DELIVERED, REFUNDED） */
    private String status;

    /** 作成日時 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 注文明細（1つの注文に複数の商品） */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItems> orderItems = new ArrayList<>();
}
>>>>>>> e5416f9e4f813fe058e65f4eff2a5fc9c03e5b53
