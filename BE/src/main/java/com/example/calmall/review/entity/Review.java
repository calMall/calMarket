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

    private Integer userId; // レビュー投稿者のユーザーID

    private String productItemCode; // 商品ID（楽天形式）

    private Integer rating; // 評価（5段階など）

    private String title; // レビュータイトル

    @Column(columnDefinition = "TEXT")
    private String comment; // コメント内容

    private String image; // 画像のURL

    private LocalDateTime createdAt; // 作成日時

    private LocalDateTime updatedAt; // 更新日時
}
