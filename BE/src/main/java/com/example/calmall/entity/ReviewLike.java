package com.example.calmall.entity;

import jakarta.persistence.*;
import lombok.Data;

// レビューに対するいいねを管理するエンティティ
@Entity
@Data
public class ReviewLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // いいねID

    private Integer userId; // いいねを押したユーザーID

    private Long reviewId; // 対象レビューID
}
