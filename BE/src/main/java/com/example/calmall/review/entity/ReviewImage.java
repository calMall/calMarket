package com.example.calmall.review.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * レビューに紐づく画像情報を管理するエンティティクラス。
 * - review_id（外部キー）でレビューと関連付けられる
 * - imageUrl: 画像の保存パス（例：/uploads/xxx.png）
 */
@Entity
@Table(name = "review_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewImage {

    // 主キーID（自動採番）
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // レビューとのリレーション（多対一）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    // 画像URL（例：/uploads/xxx.png）
    @Column(name = "image_url", nullable = false)
    private String imageUrl;
}
