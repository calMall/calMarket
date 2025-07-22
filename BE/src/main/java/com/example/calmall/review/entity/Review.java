package com.example.calmall.review.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

<<<<<<< HEAD
// 商品に対するレビューを管理するエンティティ
=======
/**
 * 商品に対するレビュー情報を管理するエンティティ
 */
>>>>>>> BE
@Entity
@Data
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
<<<<<<< HEAD
    private Long id; // レビューID

    private Integer userId; // 書き込みユーザーID

    private String itemCode ; // 楽天の商品コード

    private Integer rating; // 評価（1〜5など）

    private String title; // レビュータイトル

    @Column(columnDefinition = "TEXT")
    private String comment; // レビュー本文

    private String image; // 画像がある場合のURL

    private LocalDateTime createdAt; // 作成日時

    private LocalDateTime updatedAt; // 更新日時
=======
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
>>>>>>> BE
}
