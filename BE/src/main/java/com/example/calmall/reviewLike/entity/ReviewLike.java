package com.example.calmall.reviewLike.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * レビューに対する「いいね」を管理するエンティティ
 */
@Entity
@Data
public class ReviewLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer userId; // いいねしたユーザーのID

    private Long reviewId; // 対象のレビューID
}
