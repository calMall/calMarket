package com.example.calmall.review.entity;

import com.example.calmall.product.entity.Product;
import com.example.calmall.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品に対するレビュー情報を管理するエンティティ
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    /** レビューID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    /** 投稿ユーザー */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    private User user;

    /** 対象商品（item_code で紐付け） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_code", referencedColumnName = "item_code", nullable = false)
    private Product product;

    /** 評価（1〜5） */
    private Integer rating;

    /** レビュータイトル（任意） */
    private String title;

    /** コメント本文（必須） */
    @Column(columnDefinition = "TEXT")
    private String comment;

    /** 画像URLリスト（複数枚対応） */
    @ElementCollection
    @CollectionTable(name = "review_images", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "image_url")
    private List<String> imageList;

    /** 作成日時 */
    private LocalDateTime createdAt;

    /** 更新日時 */
    private LocalDateTime updatedAt;

    /** 論理削除フラグ（true の場合削除済み） */
    @Column(nullable = false)
    private boolean deleted;
}
