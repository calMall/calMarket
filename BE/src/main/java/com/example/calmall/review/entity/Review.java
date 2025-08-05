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
 * 商品レビューを管理するエンティティクラス
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    /** レビューID（自動採番） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    /** 投稿ユーザー（Userエンティティと多対一） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    private User user;

    /** 対象商品（Productエンティティと多対一） */
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

    /**
     * ★ 画像URLのリスト
     * - @ElementCollection を使用して別テーブル（review_image_urls）で管理
     * - review_images（画像実体テーブル）とは完全に別管理にする
     * - これにより Hibernate が review_images に INSERT することを防止
     * - 外部キーの紐付けは別途 ReviewImage エンティティ＋ネイティブSQLで行う
     */
    @ElementCollection
    @CollectionTable(
            name = "review_image_urls",
            joinColumns = @JoinColumn(name = "review_id")
    )
    @Column(name = "image_url")
    private List<String> imageList = new ArrayList<>();

    /** 作成日時（JST固定） */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Tokyo")
    private LocalDateTime createdAt;

    /** 更新日時（JST固定） */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Tokyo")
    private LocalDateTime updatedAt;

    /** 論理削除フラグ（true = 削除扱い） */
    @Column(nullable = false)
    private boolean deleted;
}
