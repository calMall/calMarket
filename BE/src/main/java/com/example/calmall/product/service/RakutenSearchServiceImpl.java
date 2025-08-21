package com.example.calmall.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * 楽天商品検索API（開発用）サービス実装。
 * 指定キーワードで IchibaItemSearch API を呼び出し、生JSON文字列を返す。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RakutenSearchServiceImpl implements RakutenSearchService {

    private final RestTemplate restTemplate;

    @Value("${rakuten.app.id}")
    private String appId;

    @Value("${rakuten.affiliate.id}")
    private String affiliateId;

    @Override
    public String searchRaw(String keyword, String shopCode, Integer hits) {
        try {
            int limit = (hits == null || hits <= 0) ? 10 : hits;

            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl("https://app.rakuten.co.jp/services/api/IchibaItem/Search/20220601")
                    .queryParam("applicationId", appId)
                    .queryParam("affiliateId", affiliateId)
                    .queryParam("keyword", keyword)
                    .queryParam("format", "json")
                    .queryParam("formatVersion", 2)
                    .queryParam("hits", limit);

            if (shopCode != null && !shopCode.isBlank()) {
                builder.queryParam("shopCode", shopCode);
            }

            URI uri = builder.build(true) // true → encoded
                    .encode(StandardCharsets.UTF_8)
                    .toUri();

            log.debug("[RakutenSearch] GET {}", uri);

            ResponseEntity<String> resp = restTemplate.getForEntity(uri, String.class);

            log.debug("[RakutenSearch] status={} length={}",
                    resp.getStatusCodeValue(),
                    resp.hasBody() ? resp.getBody().length() : 0);

            return resp.getBody() != null ? resp.getBody() : "";

        } catch (Exception e) {
            log.error("[RakutenSearch] 取得失敗 keyword={} shopCode={} : {}", keyword, shopCode, e.getMessage(), e);
            // 失敗時は簡易JSONを返す（開発用なのでシンプルに）
            return "{\"error\":\"exception\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }
}
