package com.example.calmall.product.service;

import com.example.calmall.product.entity.Product;
import com.example.calmall.product.text.DescriptionSuperCleaner; // ★ 初回から超クリーン整形
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 楽天商品APIから商品情報を取得するサービス実装クラス（超クリーン対応）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RakutenApiServiceImpl implements RakutenApiService {

    // HTTPクライアント
    private final RestTemplate restTemplate;

    // 楽天アプリID
    @Value("${rakuten.app.id}")
    private String appId;

    // アフィリエイトID（任意）
    @Value("${rakuten.affiliate.id:}")
    private String affiliateId;

    /**
     * itemCode 指定で楽天APIを叩き、最初の一致商品を Product に詰め替えて返却
     * ※ 初回保存時点で説明文を「超・クリーン化」して DB に入れる
     */
    @Override
    @SuppressWarnings("unchecked")
    public Optional<Product> fetchProductFromRakuten(String itemCode) {

        // デバッグ用：itemCode の16進表記も出す
        if (log.isDebugEnabled()) {
            StringBuilder hex = new StringBuilder();
            for (char c : itemCode.toCharArray()) hex.append(String.format("%02X ", (int) c));
            log.debug("[RakutenApi] raw itemCode='{}' hex={}", itemCode, hex);
        }

        // API URL 構築
        StringBuilder sb = new StringBuilder("https://app.rakuten.co.jp/services/api/IchibaItem/Search/20220601")
                .append("?applicationId=").append(appId)
                .append("&itemCode=").append(itemCode)
                .append("&format=json")
                .append("&formatVersion=2")
                .append("&hits=1");
        if (affiliateId != null && !affiliateId.isBlank()) {
            sb.append("&affiliateId=").append(affiliateId);
        }
        String url = sb.toString();
        log.debug("[RakutenApi] GET {}", url);

        Map<String, Object> response;
        try {
            response = restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.error("[RakutenApi] 通信失敗 itemCode={} : {}", itemCode, e.getMessage(), e);
            return Optional.empty();
        }
        if (response == null) {
            log.warn("[RakutenApi] response=null itemCode={}", itemCode);
            return Optional.empty();
        }

        Object itemsObj = response.get("Items");
        if (!(itemsObj instanceof List<?> items) || items.isEmpty()) {
            log.warn("[RakutenApi] Items空 itemCode={} keys={}", itemCode, response.keySet());
            return Optional.empty();
        }

        // v2（フラット）/ v1（Itemラッパー）両対応で Map を取り出す
        Map<String, Object> item;
        Object first = items.get(0);
        if (first instanceof Map<?, ?> m) {
            if (m.containsKey("Item")) {
                Object inner = m.get("Item");
                if (inner instanceof Map<?, ?> innerMap) {
                    item = (Map<String, Object>) innerMap;
                } else {
                    log.warn("[RakutenApi] Itemラッパー不正 type={} value={}", inner == null ? null : inner.getClass(), inner);
                    return Optional.empty();
                }
            } else {
                item = (Map<String, Object>) m;
            }
        } else {
            log.warn("[RakutenApi] Items[0]型不正 class={} value={}", first == null ? null : first.getClass(), first);
            return Optional.empty();
        }

        // ---------- Product へマッピング ----------
        Product product = new Product();
        product.setItemCode(getString(item, "itemCode"));
        product.setItemName(getString(item, "itemName"));

        // 楽天の元説明文
        String rawCaption = getString(item, "itemCaption");

        // ★ 初回から超クリーン化（DB には常に整形済みを保存）
        String cleanHtml  = DescriptionSuperCleaner.buildCleanHtml(null, null, rawCaption);
        String cleanPlain = DescriptionSuperCleaner.toPlain(cleanHtml);

        // フロント互換：itemCaption はプレーン（超クリーン）
        product.setItemCaption(cleanPlain);
        product.setDescriptionPlain(cleanPlain);
        product.setDescriptionHtml(cleanHtml);

        product.setCatchcopy(getString(item, "catchcopy"));
        product.setPrice(getInt(item, "itemPrice", 0));
        product.setItemUrl(getString(item, "itemUrl"));

        // 画像URL一覧
        List<String> images = new ArrayList<>();
        Object midObj = item.get("mediumImageUrls");
        if (midObj instanceof List<?> list) {
            for (Object img : list) {
                if (img instanceof String s) {
                    images.add(s);
                } else if (img instanceof Map<?, ?> mm) {
                    Object u = mm.get("imageUrl");
                    if (u != null) images.add(String.valueOf(u));
                }
            }
        }
        product.setImages(images);

        // 在庫などはダミー生成（デモ用途）
        product.setInventory(ThreadLocalRandom.current().nextInt(0, 301));
        product.setStatus(true);
        product.setCreatedAt(LocalDateTime.now());

        log.info("[RakutenApi] 商品取得成功 itemCode={} name={}", product.getItemCode(), product.getItemName());
        return Optional.of(product);
    }

    // -------------------- helpers --------------------

    private String getString(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return (v == null) ? null : String.valueOf(v);
    }

    private int getInt(Map<?, ?> map, String key, int fallback) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception ignore) {
            return fallback;
        }
    }
}
