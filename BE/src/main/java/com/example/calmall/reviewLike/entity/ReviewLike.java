package com.example.calmall.reviewLike.entity;

import jakarta.persistence.*;
import lombok.Data;

<<<<<<< HEAD
// レビューに対するいいねを管理するエンティティ
=======
/**
 * レビューに対する「いいね」を管理するエンティティ
 */
>>>>>>> BE
@Entity
@Data
public class ReviewLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
<<<<<<< HEAD
    private Long id; // いいねID

    private Integer userId; // いいねを押したユーザーID

    private Long reviewId; // 対象レビューID
=======
    private Long id;

    // いいねしたユーザーのID（User.id を参照）
    private Long userId;

    // 対象のレビューID
    private Long reviewId;
>>>>>>> BE
}
