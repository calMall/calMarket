package com.example.calmall.product.service;

import com.example.calmall.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 楽天商品APIから商品情報を取得するサービス実装クラス
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RakutenApiServiceImpl implements RakutenApiService {

    // RestTemplate は AppConfig などで @Bean 登録したものが自動で注入される
    private final RestTemplate restTemplate;

    @Value("${rakuten.app.id}")
    private String appId; // 楽天APIのアプリケーションID

    @Value("${rakuten.affiliate.id}")
    private String affiliateId; // アフィリエイトID

    /**
     * 指定されたitemCodeの商品を楽天APIから取得する
     * @param itemCode 楽天の商品コード
     * @return 商品情報（存在しない場合は空）
     */
    @Override
    public Optional<Product> fetchProductFromRakuten(String itemCode) {
        try {
            // 楽天の検索APIエンドポイントURLを構築
            String url = "https://app.rakuten.co.jp/services/api/IchibaItem/Search/20170706" +
                    "?applicationId=" + appId +
                    "&affiliateId=" + affiliateId +
                    "&itemCode=" + itemCode +
                    "&format=json";

            // APIを呼び出し、レスポンスをMap形式で取得
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            // レスポンスが不正または商品が存在しない場合
            if (response == null || !response.containsKey("Items")) {
                return Optional.empty();
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("Items");
            if (items.isEmpty()) return Optional.empty();

            // 商品データの中身を取り出す
            Map<String, Object> itemWrapper = items.get(0);
            Map<String, Object> item = (Map<String, Object>) itemWrapper.get("Item");

            // 楽天の商品データをProductエンティティにマッピング
            Product product = new Product();
            product.setItemCode((String) item.get("itemCode"));
            product.setItemName((String) item.get("itemName"));
            product.setItemCaption((String) item.get("itemCaption"));
            product.setCatchcopy((String) item.get("catchcopy"));
            product.setPrice((Integer) item.get("itemPrice"));
            product.setItemUrl((String) item.get("itemUrl"));

            // 画像URLリストの抽出（中サイズ）
            List<String> images = new ArrayList<>();
            List<Map<String, String>> imageList = (List<Map<String, String>>) item.get("mediumImageUrls");
            if (imageList != null) {
                for (Map<String, String> image : imageList) {
                    images.add(image.get("imageUrl"));
                }
            }
            product.setImages(images);

            // 在庫数はランダムで 0〜300 の整数に設定
            product.setInventory(new Random().nextInt(301));
            product.setStatus(true); // 販売中とする
            product.setCreatedAt(LocalDateTime.now()); // 現在時刻を保存

            return Optional.of(product);

        } catch (Exception e) {
            // エラー発生時のログ出力
            log.error("楽天API取得失敗：{}", e.getMessage());
            return Optional.empty();
        }
    }
}
