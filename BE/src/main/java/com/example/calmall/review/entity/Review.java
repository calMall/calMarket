package com.example.calmall.review.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

// 商品に対するレビューを管理するエンティティ
@Entity
@Data
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
}
