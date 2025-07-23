package com.example.calmall.product.controller;

import com.example.calmall.product.service.RakutenSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 楽天APIの検索結果をそのまま返す開発用コントローラー。
 * 本番稼働前に削除、または認証保護すること。
 */
@RestController
@RequestMapping("/api/rakuten")
@RequiredArgsConstructor
public class RakutenDevController {

    private final RakutenSearchService rakutenSearchService;

    /**
     * 開発用：キーワード検索結果の生JSONを返す。
     * 例: GET /api/rakuten/search?keyword=iphoneケース
     *     GET /api/rakuten/search?keyword=sony&shopCode=yamada&hits=5
     */
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> search(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "shopCode", required = false) String shopCode,
            @RequestParam(value = "hits", required = false) Integer hits
    ) {
        String body = rakutenSearchService.searchRaw(keyword, shopCode, hits);
        return ResponseEntity.ok(body);
    }
}
