package com.example.calmall.product.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 楽天APIの商品情報を保持するエンティティ
 */
@Entity
@Data
public class Product {

    @Id
    private String itemCode; // 楽天の商品コード（楽天形式）

    private String itemName; // 商品名（楽天形式）

    private Integer price;   // 価格

    private Integer inventory; // 在庫数（初回登録時にランダムで設定）

    @ElementCollection
    private List<String> images; // 中サイズ画像URLリスト（mediumImageUrls）

    private Boolean status = true; // 販売状態（true = 販売中）

    @ElementCollection
    private List<Integer> tagIds; // タグIDの一覧（任意）

    private String itemUrl; // 商品詳細ページへのURL

    private LocalDateTime createdAt; // 登録日時

    // 追加項目：商品説明文（楽天APIのitemCaption）
    private String itemCaption;

    // 追加項目：キャッチコピー（楽天APIのcatchcopy）
    private String catchcopy;
}
