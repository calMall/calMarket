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
 * 楽天商品APIから商品情報を取得するサービス実装クラス
 *
 * IchibaItemSearch API を itemCode 指定で呼び出し、最初の1件を Product にマッピングする簡易実装。
 * inventory は仕様に従い 0〜300 の乱数で付与。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RakutenApiServiceImpl implements RakutenApiService {

    // RestTemplate は AppConfig 等で @Bean 登録したものが注入される
    private final RestTemplate restTemplate;

    @Value("${rakuten.app.id}")
    private String appId; // 楽天APIアプリID

    @Value("${rakuten.affiliate.id:}") // 本番で未設定でも起動できるようデフォルト空文字
    private String affiliateId; // アフィリエイトID（任意）

    /**
     * 指定 itemCode の商品を楽天APIから取得し Product に変換する。
     * @param itemCode 楽天の商品コード（例：uriurishop:10005846）
     */
    @Override
    @SuppressWarnings("unchecked")
    public Optional<Product> fetchProductFromRakuten(String itemCode) {
        try {
            // ":" 等を含むコードをURLエンコード
            String encodedItemCode = URLEncoder.encode(itemCode, StandardCharsets.UTF_8);

            // APIリクエストURL生成
            StringBuilder sb = new StringBuilder("https://app.rakuten.co.jp/services/api/IchibaItem/Search/20220601")
                    .append("?applicationId=").append(appId)
                    .append("&itemCode=").append(encodedItemCode)
                    .append("&format=json");
            // affiliateId が設定されている場合のみ付与
            if (affiliateId != null && !affiliateId.isBlank()) {
                sb.append("&affiliateId=").append(affiliateId);
            }
            String url = sb.toString();

            log.debug("[RakutenApi] GET {}", url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                log.warn("[RakutenApi] レスポンスnull itemCode={}", itemCode);
                return Optional.empty();
            }

            Object itemsObj = response.get("Items");
            if (!(itemsObj instanceof List<?> items) || items.isEmpty()) {
                log.warn("[RakutenApi] Items空 itemCode={} keys={}", itemCode, response.keySet());
                return Optional.empty();
            }

            Object itemWrapperObj = items.get(0);
            if (!(itemWrapperObj instanceof Map<?, ?> itemWrapper)) {
                log.warn("[RakutenApi] itemWrapper型不正 itemCode={} obj={}", itemCode, itemWrapperObj);
                return Optional.empty();
            }

            Object itemObj = itemWrapper.get("Item");
            if (!(itemObj instanceof Map<?, ?> item)) {
                log.warn("[RakutenApi] Item型不正 itemCode={} obj={}", itemCode, itemObj);
                return Optional.empty();
            }

            // Product生成とマッピング
            Product product = new Product();
            product.setItemCode(getString(item, "itemCode"));
            product.setItemName(getString(item, "itemName"));
            product.setItemCaption(getString(item, "itemCaption"));
            product.setCatchcopy(getString(item, "catchcopy"));
            product.setPrice(getInt(item, "itemPrice", 0));
            product.setItemUrl(getString(item, "itemUrl"));

            // mediumImageUrls 抽出（formatVersion=2 の場合は String 配列）
            List<String> images = new ArrayList<>();
            Object imageListObj = item.get("mediumImageUrls");
            if (imageListObj instanceof List<?> rawList) {
                for (Object o : rawList) {
                    // V1形式(Map) / V2形式(String) 両対応
                    if (o instanceof Map<?, ?> imgMap) {
                        Object u = imgMap.get("imageUrl");
                        if (u != null) images.add(String.valueOf(u));
                    } else {
                        images.add(String.valueOf(o));
                    }
                }
            }
            product.setImages(images);

            // タグID（任意）
            Object tagIdsObj = item.get("tagIds");
            if (tagIdsObj instanceof List<?> rawList) {
                List<Integer> tags = new ArrayList<>();
                for (Object t : rawList) {
                    if (t instanceof Number n) {
                        tags.add(n.intValue());
                    } else {
                        try {
                            tags.add(Integer.parseInt(String.valueOf(t)));
                        } catch (NumberFormatException ignore) {
                        }
                    }
                }
                product.setTagIds(tags);
            }

            // 在庫（0〜300乱数）
            int inventory = ThreadLocalRandom.current().nextInt(0, 301);
            product.setInventory(inventory);

            // 販売状態（暫定: 常にtrue）
            product.setStatus(true);

            // 登録日時
            product.setCreatedAt(LocalDateTime.now());

            log.info("[RakutenApi] 商品取得成功 itemCode={} name={}", product.getItemCode(), product.getItemName());
            return Optional.of(product);

        } catch (Exception e) {
            // SSL失敗等も含め通信エラー
            log.error("[RakutenApi] 通信失敗 itemCode={} : {}", itemCode, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /* -------------------------------------------------------------
     * Map → 型変換ユーティリティ
     * ------------------------------------------------------------- */
    private String getString(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private int getInt(Map<?, ?> map, String key, int fallback) {
        Object v = map.get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception ignore) {
            return fallback;
        }
    }
}
