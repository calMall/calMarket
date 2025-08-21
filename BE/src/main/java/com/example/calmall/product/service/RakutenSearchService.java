package com.example.calmall.product.service;

public interface RakutenSearchService {


    // 楽天市場でキーワード検索を行い、JSONを返す。
    String searchRaw(String keyword, String shopCode, Integer hits);
}
