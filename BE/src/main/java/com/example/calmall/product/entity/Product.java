package com.example.calmall.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 楽天APIの商品情報を保持するエンティティ
 */
@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    /** 楽天の商品コード（例：uriurishop:10005846） - 主キー */
    @Id
    @Column(name = "item_code", nullable = false, length = 255)
    private String itemCode;

    /** 商品名（長文対応） */
    @Column(name = "item_name", columnDefinition = "text")
    private String itemName;

    /** 商品説明文 */
    @Column(name = "item_caption", columnDefinition = "text")
    private String itemCaption;

    /** キャッチコピー */
    @Column(name = "catchcopy", columnDefinition = "text")
    private String catchcopy;

    /** 価格 */
    @Column(name = "price")
    private Integer price;

    /** 在庫数 */
    @Column(name = "inventory")
    private Integer inventory;

    /** 画像URL一覧 */
    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "item_code"))
    @Column(name = "image_url", columnDefinition = "text")
    private List<String> images;

    /** 商品状態（true: 販売中） */
    @Column(name = "status")
    private Boolean status = true;

    /** タグID一覧 */
    @ElementCollection
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "item_code"))
    @Column(name = "tag_id")
    private List<Integer> tagIds;

    /** 商品詳細URL */
    @Column(name = "item_url", columnDefinition = "text")
    private String itemUrl;

    /** 作成日時 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}