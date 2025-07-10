package com.example.calmall.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

/**
 * 商品情報を管理するエンティティ
 */
@Entity
@Data
public class Product {

    @Id
    private String id; // 楽天API形式の商品ID（文字列）

    private String title; // 商品名
    private Integer price; // 価格
    private Integer inventory; // 在庫数

    @ElementCollection
    private List<String> images; // 画像URLリスト（mediumImageUrls）

    private Boolean status; // 販売状態（true=販売中、false=停止中）

    @ElementCollection
    private List<Integer> tagIds; // タグID一覧
}
