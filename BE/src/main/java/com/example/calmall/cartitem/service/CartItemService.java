package com.example.calmall.cartitem.service;

import com.example.calmall.cartitem.dto.CartAddRequestDto;
import com.example.calmall.cartitem.dto.CartListForOrderResponseDto;
import com.example.calmall.cartitem.dto.CartListResponseDto;
import com.example.calmall.cartitem.entity.CartItem;

import java.util.List;
import java.util.Optional;

/**
 * カートアイテムに関するビジネスロジックを定義するインターフェース。
 */
public interface CartItemService {

    /**
     * カートアイテムの追加または数量更新、および数量が0以下の場合の削除を行います。
     *
     * @param userId ユーザーID
     * @param requestDto カート追加/更新リクエストDTO (商品コードと数量を含む)
     * @return 更新または追加されたCartItem (削除された場合はOptional.empty())
     * @throws IllegalArgumentException 数量が負の値の場合、または商品コードが不正な場合
     */
    Optional<CartItem> addOrUpdateCartItem(String userId, CartAddRequestDto requestDto);

    /**
     * 指定されたユーザーのカートアイテムリストを取得します。
     *
     * @param userId ユーザーID
     * @return カートアイテムのリストを含むCartListResponseDto
     */
    CartListResponseDto getCartItemsForUser(String userId);

    /**
     * 指定されたカートアイテムIDを持つ商品をユーザーのカートから削除します。
     *
     * @param userId ユーザーID
     * @param cartItemId 削除するカートアイテムのID
     * @return 削除が成功した場合はtrue、見つからないかアクセス権がない場合はfalse
     */
    boolean removeCartItemById(String userId, Long cartItemId);

    /**
     * 指定されたユーザーのカート内のすべてのアイテムを削除します。
     *
     * @param userId ユーザーID
     */
    void clearCart(String userId);

    /**
     * 指定されたユーザーのカート内のアイテム数を取得します。
     *
     * @param userId ユーザーID
     * @return カートアイテムの総数
     */
    int getCartItemCount(String userId);

    /**
     * 選択されたカートアイテムIDのリストに基づいて、カートアイテムを削除します。
     * @param userId ユーザーID
     * @param cartItemIds 削除するカートアイテムのIDリスト
     */
    void removeSelectedCartItems(String userId, List<Long> cartItemIds);

     /**
     * 特定のカートアイテムの数量を更新します。
     * @param userId ユーザーID
     * @param cartItemId 更新するカートアイテムのID
     * @param newQuantity 新しい数量
     * @return 更新に成功した場合はtrue、失敗した場合はfalse
     */
    boolean updateCartItemQuantity(String userId, Long cartItemId, int newQuantity);


    boolean changeCartItemQuantity(String userId, Long cartItemId, int change);

    void removeCartItemsByItemCodes(List<String> itemCodes, String userId);

    /**
     * 注文ページで必要な、指定されたカートアイテムの情報を取得します。
     * @param userId ユーザーID
     * @param cartItemIds 注文したいカートアイテムのIDリスト
     * @return 注文ページ用のカートアイテムリストを含むDTO
     */
    CartListForOrderResponseDto getCartItemsForOrderPage(String userId, List<Long> cartItemIds);

}
