package com.example.calmall.orders.repository;

import com.example.calmall.orders.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 注文履歴を扱うJPAリポジトリ
 */
public interface OrdersRepository extends JpaRepository<Orders, Long> {

    /**
     * 指定された userId のユーザーが、
     * 指定された itemCode の商品を
     * 指定日時以降に購入した履歴があるかを確認
     *
     * @param userId ユーザーの UUID（User.userId）
     * @param itemCode 商品の itemCode（Product.itemCode）
     * @param after 検索基準となる日時（1ヶ月前など）
     * @return true = 購入履歴あり / false = 購入なし
     */
    boolean existsByUser_UserIdAndProduct_ItemCodeAndCreatedAtAfter(
            String userId,
            String itemCode,
            LocalDateTime after
    );

    /**
     * 指定された userId のユーザーが、
     * 指定された itemCode の商品を購入した履歴をすべて取得
     *
     * @param userId ユーザーの UUID（User.userId）
     * @param itemCode 商品の itemCode（Product.itemCode）
     * @return 該当する注文履歴リスト
     */
    List<Orders> findByUser_UserIdAndProduct_ItemCode(String userId, String itemCode);
}
