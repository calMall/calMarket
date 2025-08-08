package com.example.calmall.cartitem.controller;

import com.example.calmall.cartitem.dto.CartAddRequestDto;
import com.example.calmall.cartitem.dto.CartListForOrderResponseDto;
import com.example.calmall.cartitem.dto.CartListResponseDto;
import com.example.calmall.cartitem.entity.CartItem;
import com.example.calmall.cartitem.service.CartItemService;
import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * カート機能に関するAPIエンドポイントを提供するコントローラー。
 */
@RestController // RESTful APIコントローラーであることを示す
@RequestMapping("/api/cart") // ベースパスを設定
@RequiredArgsConstructor // finalフィールドのコンストラクタを自動生成
@Slf4j // ロギング用のアノテーション
public class CartController {

    private final CartItemService cartItemService;
    private String getUserIdFromSession(HttpSession session) {
        // セッションが存在しない場合
        if (session == null) {
            log.warn("セッションが存在しません。");
            return null;
        }

        // セッションからユーザー情報を取得
        User user = (User) session.getAttribute("user");
        if (user == null) {
            log.warn("セッションにユーザー情報が存在しません。");
            return null;
        }

        // ユーザーIDを取得
        String userId = user.getUserId();
        if (userId == null) {
            log.warn("セッションにユーザーIDが見つかりませんでした。");
            return null;
        }

        return userId;
    }


    /**
     * カートに商品を追加、または既存商品の数量を調整します。
     * 数量が0になった場合は商品を削除します。
     *
     * @param requestDto カート追加/更新リクエストDTO
     * @param request HttpServletRequest (ユーザーID取得のため)
     * @return 成功または失敗を示すApiResponseDto
     */
    @PostMapping
    public ResponseEntity<ApiResponseDto> addOrUpdateCartItem(@RequestBody CartAddRequestDto requestDto,
                                                              HttpServletRequest request,
                                                              HttpSession session) {
        String userId  = getUserIdFromSession(session);
        if (userId == null) {
            log.warn("ログインが必要です。");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDto("ログインが必要です"));
        }

        try {
            Optional<CartItem> result = cartItemService.addOrUpdateCartItem(userId, requestDto);
            if (result.isPresent()) {
                log.info("カートアイテムが追加または更新されました。userId={}, itemCode={}, quantity={}", userId, requestDto.getItemCode(), requestDto.getQuantity());
                return ResponseEntity.ok(new ApiResponseDto("success"));
            } else {
                // 数量が0になり削除された場合も成功とみなす
                log.info("カートアイテムが削除されました (数量0)。userId={}, itemCode={}", userId, requestDto.getItemCode());
                return ResponseEntity.ok(new ApiResponseDto("success"));
            }
        } catch (IllegalArgumentException e) {
            log.error("カート追加/更新リクエストの引数エラー: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDto("fail: " + e.getMessage()));
        } catch (Exception e) {
            log.error("カート操作中に予期せぬエラーが発生しました。userId={}, requestDto={}: {}", userId, requestDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDto("fail: カートの操作に失敗しました"));
        }
    }

    /**
     * ログイン中のユーザーのカート内容を取得します。
     *
     * @param request HttpServletRequest (ユーザーID取得のため)
     * @return カートアイテムのリストを含むCartListResponseDto
     */
    @GetMapping
    public ResponseEntity<CartListResponseDto> getCartItems(HttpServletRequest request,
                                                            HttpSession session) {
        String userId = getUserIdFromSession(session);
        if (userId == null) {
            log.warn("ログインが必要です。");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CartListResponseDto.builder()
                            .message("fail")
                            .cartItems(Collections.emptyList()) // 空のリストを返す
                            .build()
            );
        }

        try {
            CartListResponseDto response = cartItemService.getCartItemsForUser(userId);
            log.info("ユーザーのカート内容を取得しました。userId={}, itemsCount={}", userId, response.getCartItems().size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("カート内容取得中に予期せぬエラーが発生しました。userId={}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CartListResponseDto.builder()
                            .message("fail")
                            .cartItems(Collections.emptyList()) // エラー時は空のリストを返す
                            .build()
            );
        }
    }

    /**
     * カートから特定のアイテムを削除します。
     *
     * @param cartItemId 削除するカートアイテムのID
     * @param request HttpServletRequest (ユーザーID取得のため)
     * @return 成功または失敗を示すApiResponseDto
     */
 /**
     * 特定のカートアイテムの数量を更新します。
     *
     * @param cartItemId 更新するカートアイテムのID
     * @param newQuantity 新しい数量
     * @param session セッション
     * @return 成功または失敗を示すApiResponseDto
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponseDto> updateCartItemQuantity(
            @PathVariable("id") Long cartItemId,
            @RequestParam("quantity") int newQuantity,
            HttpSession session) {
        String userId = getUserIdFromSession(session);
        if (userId == null) {
            log.warn("ログインが必要です。");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDto("ログインが必要です"));
        }

        try {
            boolean updated = cartItemService.updateCartItemQuantity(userId, cartItemId, newQuantity);
            if (updated) {
                log.info("カートアイテムの数量が更新されました。userId={}, cartItemId={}, newQuantity={}", userId, cartItemId, newQuantity);
                return ResponseEntity.ok(new ApiResponseDto("success"));
            } else {
                log.warn("カートアイテムの数量更新に失敗しました。指定された商品が見つからないか、アクセス権がありません。userId={}, cartItemId={}", userId, cartItemId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDto("fail: 指定された商品がカートに見つからないか、アクセス権がありません"));
            }
        } catch (IllegalArgumentException e) {
            log.error("数量更新リクエストの引数エラー: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDto("fail: " + e.getMessage()));
        } catch (Exception e) {
            log.error("カートアイテム数量更新中に予期せぬエラーが発生しました。userId={}, cartItemId={}: {}", userId, cartItemId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDto("fail: カート商品の数量更新に失敗しました"));
        }
    }

    /**
     * 特定のカートアイテムの数量を1つ増やします。
     */
    @PatchMapping("/{id}/increase")
    public ResponseEntity<ApiResponseDto> increaseCartItemQuantity(@PathVariable("id") Long cartItemId, HttpSession session) {
        String userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDto("ログインが必要です"));
        }
        try {
            cartItemService.changeCartItemQuantity(userId, cartItemId, 1); // +1 をサービスに渡す
            return ResponseEntity.ok(new ApiResponseDto("success"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDto("fail: 数量更新に失敗しました"));
        }
    }

    /**
     * 特定のカートアイテムの数量を1つ減らします。
     */
    @PatchMapping("/{id}/decrease")
    public ResponseEntity<ApiResponseDto> decreaseCartItemQuantity(@PathVariable("id") Long cartItemId, HttpSession session) {
        String userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDto("ログインが必要です"));
        }
        try {
            cartItemService.changeCartItemQuantity(userId, cartItemId, -1); // -1 をサービスに渡す
            return ResponseEntity.ok(new ApiResponseDto("success"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDto("fail: 数量更新に失敗しました"));
        }
    }

    /**
     * ログイン中のユーザーのカート内のすべてのアイテムを一括削除します。
     *
     * @param request HttpServletRequest (ユーザーID取得のため)
     * @return 成功または失敗を示すApiResponseDto
     */
    /* 
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponseDto> clearUserCart(HttpServletRequest request,
                                                        HttpSession session) {
        String userId  = getUserIdFromSession(session);

        if (userId == null) {
            log.warn("ログインが必要です。");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDto("ログインが必要です"));
        }

        try {
            cartItemService.clearCart(userId);
            log.info("ユーザーのカートがクリアされました。userId={}", userId);
            return ResponseEntity.ok(new ApiResponseDto("success"));
        } catch (Exception e) {
            log.error("カートクリア中に予期せぬエラーが発生しました。userId={}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDto("fail: カートのクリアに失敗しました"));
        }
    }
    */
    //選択削除
    @PostMapping("/remove-selected")
    public ResponseEntity<ApiResponseDto> removeSelectedCartItems(@RequestBody List<Long> cartItemIds,HttpSession session) {
        String userId = getUserIdFromSession(session);
        if (userId == null) {
            log.warn("ログインが必要です。");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDto("ログインが必要です"));
        }

        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDto("fail: 削除するアイテムIDが指定されていません"));
        }
        try {
            cartItemService.removeSelectedCartItems(userId, cartItemIds);
            log.info("選択されたカートアイテムが削除されました。userId={}, cartItemIds={}", userId, cartItemIds);
            return ResponseEntity.ok(new ApiResponseDto("success"));
        } catch (IllegalArgumentException e) {
            log.error("選択アイテム削除中に引数エラーが発生しました。userId={}, cartItemIds={}: {}", userId, cartItemIds, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDto("fail: " + e.getMessage()));
        } catch (Exception e) {
            log.error("選択アイテム削除中に予期せぬエラーが発生しました。userId={}, cartItemIds={}: {}", userId, cartItemIds, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDto("fail: 選択商品の削除に失敗しました"));
        }
    }

 @PostMapping("/list-for-order")
public ResponseEntity<CartListForOrderResponseDto> getCartItemsForOrderPage(@RequestBody List<Long> cartItemIds, HttpSession session) {
    // セッションからユーザーIDを取得
    String userId = getUserIdFromSession(session);
    if (userId == null) {
        log.warn("ログインが必要です。");
        // 401 Unauthorized と、エラーメッセージを含むDTOを返す
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(CartListForOrderResponseDto.builder().message("fail: ログインが必要です").cartList(Collections.emptyList()).build());
    }

    // カートアイテムIDが指定されているか確認
    if (cartItemIds == null || cartItemIds.isEmpty()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(CartListForOrderResponseDto.builder().message("fail: アイテムIDが指定されていません").cartList(Collections.emptyList()).build());
    }

    try {
        // サービス層から、注文ページに必要なカートアイテム情報を取得
        CartListForOrderResponseDto response = cartItemService.getCartItemsForOrderPage(userId, cartItemIds);
        
        log.info("注文ページ用のカートアイテムリストを取得しました。userId={}, cartItemIds={}", userId, cartItemIds);
        
        // 成功した場合は200 OKとレスポンスDTOを返す
        return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
        log.error("引数エラー: userId={}, cartItemIds={}: {}", userId, cartItemIds, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(CartListForOrderResponseDto.builder().message("fail: " + e.getMessage()).cartList(Collections.emptyList()).build());
    } catch (Exception e) {
        log.error("予期せぬエラー: userId={}, cartItemIds={}: {}", userId, cartItemIds, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(CartListForOrderResponseDto.builder().message("fail: カート情報の取得に失敗しました").cartList(Collections.emptyList()).build());
    }
}
}
