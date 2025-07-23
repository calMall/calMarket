package com.example.calmall.product.service;

import com.example.calmall.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class RakutenApiServiceImpl implements RakutenApiService {

    private final RestTemplate restTemplate;

    @Value("${rakuten.app.id}")
    private String appId;

    @Value("${rakuten.affiliate.id:}")
    private String affiliateId;

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Product> fetchProductFromRakuten(String itemCode) {
        try {
            // コロン等はエンコードせずそのまま送る（エンコードすると楽天側が wrong_parameter になる）
            StringBuilder sb = new StringBuilder("https://app.rakuten.co.jp/services/api/IchibaItem/Search/20220601")
                    .append("?applicationId=").append(appId)
                    .append("&itemCode=").append(itemCode)
                    .append("&format=json");
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

            Product product = new Product();
            product.setItemCode(getString(item, "itemCode"));
            product.setItemName(getString(item, "itemName"));
            product.setItemCaption(getString(item, "itemCaption"));
            product.setCatchcopy(getString(item, "catchcopy"));
            product.setPrice(getInt(item, "itemPrice", 0));
            product.setItemUrl(getString(item, "itemUrl"));

            // mediumImageUrls
            List<String> images = new ArrayList<>();
            Object imageListObj = item.get("mediumImageUrls");
            if (imageListObj instanceof List<?> rawList) {
                for (Object o : rawList) {
                    if (o instanceof Map<?, ?> imgMap) {
                        Object u = imgMap.get("imageUrl");
                        if (u != null) images.add(String.valueOf(u));
                    } else {
                        images.add(String.valueOf(o));
                    }
                }
            }
            product.setImages(images);

            // tagIds
            Object tagIdsObj = item.get("tagIds");
            if (tagIdsObj instanceof List<?> rawList) {
                List<Integer> tags = new ArrayList<>();
                for (Object t : rawList) {
                    if (t instanceof Number n) tags.add(n.intValue());
                    else {
                        try { tags.add(Integer.parseInt(String.valueOf(t))); } catch (NumberFormatException ignore) {}
                    }
                }
                product.setTagIds(tags);
            }

            int inventory = ThreadLocalRandom.current().nextInt(0, 301);
            product.setInventory(inventory);
            product.setStatus(true);
            product.setCreatedAt(LocalDateTime.now());

            log.info("[RakutenApi] 商品取得成功 itemCode={} name={}", product.getItemCode(), product.getItemName());
            return Optional.of(product);

        } catch (Exception e) {
            log.error("[RakutenApi] 通信失敗 itemCode={} : {}", itemCode, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private String getString(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v == null ? null : String.valueOf(v);
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
