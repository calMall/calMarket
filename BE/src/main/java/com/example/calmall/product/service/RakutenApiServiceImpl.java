package com.example.calmall.product.service;

import com.example.calmall.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 楽天商品APIから商品情報を取得するサービス実装クラス。
 *
 * IchibaItemSearch API を itemCode 指定で呼び出し、結果を Product に変換する。
 * ・APIバージョンは 20220601 を使用（推奨）
 * ・formatVersion=2 を指定（新フォーマット）
 * ・旧フォーマット（Items / Item wrapper）にもフォールバック対応
 * ・在庫数は仕様に従い 0〜300 の乱数を付与
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RakutenApiServiceImpl implements RakutenApiService {

    /** RestTemplate は AppConfig などで @Bean 登録したものが自動注入される */
    private final RestTemplate restTemplate;

    @Value("${rakuten.app.id}")
    private String appId; // 楽天APIのアプリケーションID

    @Value("${rakuten.affiliate.id}")
    private String affiliateId; // アフィリエイトID

    /**
     * 指定された itemCode の商品を楽天APIから取得して Product に変換する。
     * @param itemCode 楽天の商品コード（例：blessyou:cc4005-63l）
     * @return 商品情報（取得できなければ Optional.empty()）
     */
    @Override
    @SuppressWarnings("unchecked")
    public Optional<Product> fetchProductFromRakuten(String itemCode) {
        try {
            // itemCode に ":" 等が含まれる場合に備えて URL エンコード
            String encodedItemCode = URLEncoder.encode(itemCode, StandardCharsets.UTF_8);

            // 楽天 IchibaItemSearch API リクエストURL（最新版推奨）
            String url = "https://app.rakuten.co.jp/services/api/IchibaItem/Search/20220601"
                    + "?applicationId=" + appId
                    + "&affiliateId=" + affiliateId
                    + "&itemCode=" + encodedItemCode
                    + "&format=json"
                    + "&formatVersion=2"   // 新フォーマット
                    + "&hits=1";           // 1件だけ取得

            log.debug("[RakutenApi] Request URL: {}", url);

            // APIコール
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                log.warn("[RakutenApi] response == null");
                return Optional.empty();
            }

            // ----- Items / items 両対応 -----
            Object itemsObj = response.get("Items");
            if (itemsObj == null) {
                itemsObj = response.get("items"); // formatVersion=2 の場合はこちら
            }
            if (!(itemsObj instanceof List<?> rawItems) || rawItems.isEmpty()) {
                log.warn("[RakutenApi] Items(大小写)なし itemCode={} keys={}", itemCode, response.keySet());
                return Optional.empty();
            }

            // 先頭要素を取り出す（旧：wrapper / 新：直接Item）
            Object first = rawItems.get(0);
            Map<String, Object> itemMap;

            if (first instanceof Map<?, ?> m) {
                // 旧フォーマット {"Item":{...}}
                Object inner = m.get("Item");
                if (inner instanceof Map<?, ?> mm) {
                    itemMap = (Map<String, Object>) mm;
                } else {
                    // 新フォーマット そのまま商品本体
                    itemMap = (Map<String, Object>) m;
                }
            } else {
                log.warn("[RakutenApi] item レコード型不正: {}", first);
                return Optional.empty();
            }

            // Product 生成
            Product product = new Product();
            product.setItemCode(getString(itemMap, "itemCode"));
            product.setItemName(getString(itemMap, "itemName"));
            product.setItemCaption(getString(itemMap, "itemCaption"));
            product.setCatchcopy(getString(itemMap, "catchcopy"));
            product.setPrice(getInt(itemMap, "itemPrice", 0));
            product.setItemUrl(getString(itemMap, "itemUrl"));

            // 画像URL（中サイズ）抽出
            List<String> images = new ArrayList<>();
            Object imageListObj = itemMap.get("mediumImageUrls");
            if (imageListObj instanceof List<?> rawList) {
                for (Object o : rawList) {
                    if (o instanceof Map<?, ?> imgMap) {
                        Object urlObj = imgMap.get("imageUrl");
                        if (urlObj != null) {
                            images.add(String.valueOf(urlObj));
                        }
                    }
                }
            }
            product.setImages(images);

            // 在庫 0〜300
            int inventory = ThreadLocalRandom.current().nextInt(0, 301);
            product.setInventory(inventory);

            // 仮：常に販売中
            product.setStatus(true);

            // 登録日時（取得時刻）
            product.setCreatedAt(LocalDateTime.now());

            log.info("[RakutenApi] 商品取得成功 itemCode={} name={} inventory={}",
                    product.getItemCode(), product.getItemName(), inventory);

            return Optional.of(product);

        } catch (Exception e) {
            log.error("楽天API取得失敗 itemCode={} : {}", itemCode, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /* ===== 以下ユーティリティ ===== */

    /** Map から String を取り出す（null可） */
    private String getString(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return (v == null) ? null : String.valueOf(v);
    }

    /** Map から int を取り出す（失敗時 fallback） */
    private int getInt(Map<?, ?> map, String key, int fallback) {
        Object v = map.get(key);
        if (v instanceof Number num) return num.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception ignore) {
            return fallback;
        }
    }
}
