package com.example.calmall.review.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


// レビューに紐づく画像情報を管理するエンティティ
@Entity
@Table(name = "review_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewImage {

    // 主キーID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // レビューとのリレーション（多対一、投稿後に後付け紐付け）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = true)
    private Review review;

    // 画像URL
    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    // Cloudinary の public_id削除や更新で利用
    @Column(name = "public_id")
    private String publicId;

    // コンテンツタイプ
    @Column(name = "content_type", nullable = false)
    private String contentType;

    // アップロード日時
    @Column(name = "created_at", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
