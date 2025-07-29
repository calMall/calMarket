// ファイルパス: com.example.calmall.review.entity.ReviewImage.java

package com.example.calmall.review.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * アップロードされた画像のURL情報を管理するエンティティ
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "review_images")
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 画像URL（/uploads/xxxxx.jpg）
    @Column(nullable = false)
    private String imageUrl;

    // 画像のタイプ（image/png）
    private String contentType;

    // 作成日時
    private String createdAt;
}
