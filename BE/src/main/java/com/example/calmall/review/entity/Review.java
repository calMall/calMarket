package com.example.calmall.review.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品に対するレビュー情報を管理するエンティティ
 */
@Entity
@Data
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // レビュー投稿者のユーザーID（User.id を参照）
    private Long userId;

    // 商品ID（楽天形式）
    private String productItemCode;

    // 評価（5段階など）
    private Integer rating;

    // レビュータイトル
    private String title;

    // コメント内容
    @Column(columnDefinition = "TEXT")
    private String comment;

    // 画像のURL
    private String image;

    // 作成日時
    private LocalDateTime createdAt;

    // 更新日時
    private LocalDateTime updatedAt;
}
