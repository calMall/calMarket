package com.example.calmall.review.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * レビューに紐づく画像情報を管理するエンティティクラス。
 * - review_id（外部キー）でレビューと関連付けられる
 * - imageUrl: 画像の保存パス（例：/uploads/xxx.png）
 * - contentType: 画像のMIMEタイプ（例：image/png）
 */
@Entity
@Table(name = "review_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewImage {

    /** 主キーID（自動採番） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** レビューとのリレーション（多対一、投稿後に後付け紐付け） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = true)
    private Review review;

    /** 画像URL（保存先のファイルパスやURL） */
    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    /** コンテンツタイプ（例：image/png） */
    @Column(name = "content_type", nullable = false)
    private String contentType;

    /** アップロード日時 */
    @Column(name = "created_at", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
