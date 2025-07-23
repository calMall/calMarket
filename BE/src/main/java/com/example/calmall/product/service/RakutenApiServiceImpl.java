package com.example.calmall.product.service;

import com.example.calmall.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 楽天市場商品検索API（IchibaItemSearch）を用いて
 * itemCode で単一商品情報を取得する最小実装クラス。
 *
 * ・送信パラメータは applicationId / itemCode / format=json のみ（テスト簡略化）。
 * ・affiliateId は任意設定（空なら送信しない）。
 * ・レスポンスフォーマットはバージョンにより
 *   Items[0].Item（旧形式）または Items[0]（フラット）の両対応。
 * ・在庫数は仕様に従い 0～300 の乱数で付与。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RakutenApiServiceImpl implements RakutenApiService {

    // RestTemplate（AppConfigで@Bean登録済み想定）
    private final RestTemplate restTemplate;

    @Value("${rakuten.app.id}")
    private String appId; // 楽天APIアプリケーションID（必須）

    // 空文字をデフォルトにしておくことで未設定時も起動失敗を避ける
    @Value("${rakuten.affiliate.id:}")
    private String affiliateId; // アフィリエイトID（任意）

    // もっとも新しい API バージョン（20220601）を使用
    private static final String BASE_URL = "https://app.rakuten.co.jp/services/api/IchibaItem/Search/20220601";

    /**
     * 指定された itemCode の商品を楽天APIから取得し、Product エンティティに変換して返す。
     * @param itemCode 楽天商品コード（例：shopcode:商品管理番号）
     * @return 取得できた場合は Product、失敗時は空
     */
    @Override
    @SuppressWarnings("unchecked")
    public Optional<Product> fetchProductFromRakuten(String itemCode) {

        // --- リクエストURL生成（SpringにURLエンコードを委譲） ---
        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("applicationId", appId)
                .queryParam("itemCode", itemCode)
                .queryParam("format", "json");
        if (affiliateId != null && !affiliateId.isBlank()) {
            ub.queryParam("affiliateId", affiliateId);
        }
        URI uri = ub.build(true).toUri();
        log.debug("[RakutenApi] GET {}", uri);

        Map<String, Object> response;
        try {
            response = restTemplate.getForObject(uri, Map.class);
        } catch (HttpClientErrorException e) {
            // wrong_parameter 等で 400 の場合はこちらに入る
            log.error("[RakutenApi] 取得失敗 itemCode={} : {} {}", itemCode, e.getStatusCode().value(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.error("[RakutenApi] 通信失敗 itemCode={} : {}", itemCode, e.getMessage(), e);
            return Optional.empty();
        }

        if (response == null) {
            log.warn("[RakutenApi] レスポンスnull itemCode={}", itemCode);
            return Optional.empty();
        }

        Object itemsObj = response.get("Items");
        if (!(itemsObj instanceof List<?> items) || items.isEmpty()) {
            log.warn("[RakutenApi] Items空 itemCode={} keys={}", itemCode, response.keySet());
            return Optional.empty();
        }

        // --- 旧形式 or 新形式 判定 ---
        Map<String, Object> itemMap;
        Object first = items.get(0);
        if (first instanceof Map<?, ?> m && m.containsKey("Item")) {
            // 旧形式（Items[].Item）
            Object inner = m.get("Item");
            if (!(inner instanceof Map)) {
                log.warn("[RakutenApi] Itemラッパ形式不正 itemCode={}", itemCode);
                return Optional.empty();
            }
            itemMap = (Map<String, Object>) inner;
        } else if (first instanceof Map) {
            // 新形式（フラット）
            itemMap = (Map<String, Object>) first;
        } else {
            log.warn("[RakutenApi] Items[0]型不正 itemCode={}", itemCode);
            return Optional.empty();
        }

        // --- Product へ詰め替え ---
        Product p = new Product();
        p.setItemCode(str(itemMap, "itemCode"));
        p.setItemName(str(itemMap, "itemName"));
        p.setItemCaption(str(itemMap, "itemCaption"));
        p.setCatchcopy(str(itemMap, "catchcopy"));
        p.setPrice(intVal(itemMap, "itemPrice", 0));
        p.setItemUrl(str(itemMap, "itemUrl"));

        // mediumImageUrls: 旧=List<Map{imageUrl:..}> / 新=List<String>
        List<String> images = new ArrayList<>();
        Object mi = itemMap.get("mediumImageUrls");
        if (mi instanceof List<?> raw) {
            for (Object o : raw) {
                if (o instanceof String s) {
                    images.add(s);
                } else if (o instanceof Map<?, ?> mm) {
                    Object u = mm.get("imageUrl");
                    if (u != null) images.add(String.valueOf(u));
                }
            }
        }
        p.setImages(images);

        // 在庫（0～300）
        p.setInventory(ThreadLocalRandom.current().nextInt(0, 301));
        p.setStatus(true);
        p.setCreatedAt(LocalDateTime.now());

        log.info("[RakutenApi] 商品取得成功 itemCode={} name={}", p.getItemCode(), p.getItemName());
        return Optional.of(p);
    }

    /* --------------------------- ユーティリティ --------------------------- */

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : String.valueOf(v);
    }

    private int intVal(Map<String, Object> m, String k, int fallback) {
        Object v = m.get(k);
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); }
        catch (Exception ignore) { return fallback; }
    }
}
