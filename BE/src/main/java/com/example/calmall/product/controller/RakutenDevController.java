package com.example.calmall.product.controller;

import com.example.calmall.product.service.RakutenSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


// 楽天APIの検索結果をそのまま返す
@RestController
@RequestMapping("/api/rakuten")
@RequiredArgsConstructor
public class RakutenDevController {

    private final RakutenSearchService rakutenSearchService;


    // キーワード検索結果の生JSONを返す。
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
