package com.example.calmall.product.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 楽天APIの商品情報を保持するエンティティ
 * <p>
 * ※ 本クラスは「開発環境でのスキーマ再構築（重建）」を前提に、
 *    長文が想定される文字列カラムをすべて PostgreSQL の TEXT 型に変更している。
 *    （従来の varchar(255) では楽天APIのレスポンス長に耐えられずエラーとなるため）
 * </p>
 */
@Entity
@Table(name = "product") // 明示的にテーブル名指定（省略可）
@Data
public class Product {

    /** 楽天の商品コード（例：uriurishop:10005846） */
    @Id
    @Column(name = "item_code", nullable = false, length = 255) // コードは比較的短いため varchar のままでも可
    private String itemCode;

    /** 商品名（長文対応：TEXT） */
    @Column(name = "item_name", columnDefinition = "text")
    private String itemName;

    /** 商品説明文（楽天API itemCaption）長文対応：TEXT */
    @Column(name = "item_caption", columnDefinition = "text")
    private String itemCaption;

    /** キャッチコピー（楽天API catchcopy）長文対応：TEXT */
    @Column(name = "catchcopy", columnDefinition = "text")
    private String catchcopy;

    /** 価格 */
    @Column(name = "price")
    private Integer price;

    /** 在庫数（初回登録時にランダムで設定） */
    @Column(name = "inventory")
    private Integer inventory;

    /**
     * 中サイズ画像URLリスト（mediumImageUrls）
     * URLが長くなる可能性があるため TEXT 指定。
     * ElementCollection 用の別テーブルを明示定義。
     */
    @ElementCollection
    @CollectionTable(
            name = "product_images",
            joinColumns = @JoinColumn(name = "item_code")  // 親テーブルの主キーと連結
    )
    @Column(name = "image_url", columnDefinition = "text")
    private List<String> images;

    /** 販売状態（true = 販売中） */
    @Column(name = "status")
    private Boolean status = true;

    /**
     * タグID一覧（任意）
     * 数値なので通常の integer 列で十分。
     */
    @ElementCollection
    @CollectionTable(
            name = "product_tags",
            joinColumns = @JoinColumn(name = "item_code")
    )
    @Column(name = "tag_id")
    private List<Integer> tagIds;

    /** 商品詳細ページへのURL（長くなるため TEXT） */
    @Column(name = "item_url", columnDefinition = "text")
    private String itemUrl;

    /** 登録日時 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
