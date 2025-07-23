package com.example.calmall.product.service;

/**
 * 楽天商品検索API（キーワード検索）をラップする開発用サービス。
 * キーワードで検索し、楽天からの生JSON文字列を返す。
 */
public interface RakutenSearchService {

    /**
     * 楽天市場でキーワード検索を行い、生JSONを返す。
     *
     * @param keyword  検索キーワード（UTF-8でURLエンコードされる）
     * @param shopCode ショップコード（任意。null可）
     * @param hits     取得件数（1～30程度を推奨。null時はデフォルト10）
     * @return 楽天APIのレスポンスJSON文字列（失敗時はエラーボディ文字列）
     */
    String searchRaw(String keyword, String shopCode, Integer hits);
}
