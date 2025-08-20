package com.example.calmall.review.entity;

import com.example.calmall.product.entity.Product;
import com.example.calmall.user.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品に対するレビュー情報を管理するエンティティクラス
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    // レビューID（自動採番）
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    // 投稿ユーザー（Userエンティティと多対一）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    private User user;

    // 対象商品（Productエンティティと多対一、itemCodeで紐付け）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_code", referencedColumnName = "item_code", nullable = false)
    private Product product;

    //評価（1〜5）
    private Integer rating;

    //レビュータイトル（任意）
    private String title;

    // コメント本文（必須）
    @Column(columnDefinition = "TEXT")
    private String comment;


    // レビューに関連付けられた画像URL一覧
    @Transient
    @Builder.Default
    private List<String> imageList = new ArrayList<>();

    // 作成日時（JST固定）
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Tokyo")
    private LocalDateTime createdAt;

    // 更新日時（JST固定）
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Tokyo")
    private LocalDateTime updatedAt;

    // 論理削除フラグ（true の場合は削除扱い）
    @Column(nullable = false)
    private boolean deleted;
}
